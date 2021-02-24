package fr.guihardbastien.boilerplate;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TaggedBuffer<T> {
    private int arraySize = 4;
    private int eltSize = 0;
    private int filteredEltSize = 0;

    private final Predicate<? super T> predicate;
    private T[] elements = (T[]) new Object[arraySize];

    public TaggedBuffer(Predicate<? super T> fun) {
        Objects.requireNonNull(fun);
        this.predicate = fun;
    }

    private void grow() {
        if (eltSize == arraySize) {
            this.arraySize *= 2;
            this.elements = Arrays.copyOf(this.elements, this.arraySize);
        }
    }

    public List<T> asTaggedList() {

        var indexesArray = new int[filteredEltSize];
        var j = 0;

        for (int i = 0; i < eltSize; i++) {
            if (predicate.test(elements[i])) {
                indexesArray[j] = i;
                j++;
            }
        }

        class TaggedList extends AbstractList<T> implements RandomAccess {
            private final int[] filteredIndexes = indexesArray;

            @Override
            public T get(int index) {
                return TaggedBuffer.this.elements[filteredIndexes[index]];
            }

            @Override
            public int size() {
                return filteredIndexes.length;
            }
        }
        return new TaggedList();
    }

    public void add(T elt) {
        Objects.requireNonNull(elt);
        grow();
        elements[eltSize] = elt;
        if (this.predicate.test(elt)) {
            filteredEltSize++;
        }
        eltSize++;
    }

    public int size(boolean onlyTagged) {
        if (onlyTagged) {
            return this.filteredEltSize;
        } else {
            return this.eltSize;
        }
    }

    public Optional<T> findFirst(boolean onlyTagged) {
        if (onlyTagged) {
            return Arrays.stream(elements)
                    .filter(Objects::nonNull)
                    .filter(predicate)
                    .findFirst();
        } else {
            return Optional.ofNullable(elements[0]);
        }
    }

    public void forEach(boolean onlyTagged, Consumer<? super T> fun) {
        if (onlyTagged) {
            // boucle !! jusqu au max. ou la fonction de stream
            Arrays.stream(elements).filter(Objects::nonNull).filter(predicate).forEach(fun);
        } else {
            Arrays.stream(elements).filter(Objects::nonNull).forEach(fun);
        }
    }

    public Iterator<T> iterator(boolean onlyTagged) {

        return new Iterator<T>() {
            private final int maxSize = eltSize;
            private final int maxTaggedElements = filteredEltSize;
            private int startLookup = 0; // findNext(onlyTagged, 0); // index of the next element to be returned by next
            private int nbReturn;

            private int findNext(boolean onlyTagged, int startIncluded) {
                if (onlyTagged) {
                    var i = startIncluded;
                    while (i < maxSize) {
                        if (predicate.test(elements[i])) {
                            return i;
                        }
                        i++;
                    }
                    return maxSize;
                } else {
                    return startIncluded;
                }
            }

            @Override
            public boolean hasNext() {
                if (onlyTagged) {
                    return nbReturn < maxTaggedElements;
                } else {
                    return nbReturn < maxSize;
                }
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                var next = findNext(onlyTagged, startLookup);
                T elt = elements[next]; // element to be returned
                startLookup = next + 1;
                nbReturn++;
                return elt;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public Stream<T> stream(boolean onlyTagged) {
        if (onlyTagged) {
            return StreamSupport.stream(createSpliteratorOnlyTagged(0, this.eltSize, this.elements), true);
        } else {
            return StreamSupport.stream(createSpliterator(0, this.eltSize, this.elements), true);
        }
    }

    private Spliterator<T> createSpliteratorOnlyTagged(int start, int end, T[] array) {

        return new Spliterator<T>() {

            private int i = start;

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                while (i != end) {
                    if (predicate.test(elements[i])) {
                        consumer.accept(elements[i]);
                        i++;
                        return true;
                    }
                    i++;
                }
                return false;
            }

            @Override
            public Spliterator<T> trySplit() {
                var middle = (i + end) >> 1;
                if (middle == i) {
                    return null;
                }
                var spliterator = createSpliteratorOnlyTagged(i, middle, array);
                i = middle;
                return spliterator;
            }

            @Override
            public long estimateSize() {
                return end - i;
            }

            @Override
            public int characteristics() {
                return NONNULL | ORDERED;
            }
        };
    }

    private Spliterator<T> createSpliterator(int start, int end, T[] array) {

        return new Spliterator<T>() {
            private int i = start;

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                if (i < end) {
                    consumer.accept(array[i++]);
                    return true;
                }
                return false;
            }

            @Override
            public Spliterator<T> trySplit() {
                var middle = (i + end) >> 1;
                if (middle == i) {
                    return null;
                }
                var spliterator = createSpliterator(i, middle, array);
                i = middle;
                return spliterator;
            }

            @Override
            public long estimateSize() {
                return end - i;
            }

            @Override
            public int characteristics() {
                return NONNULL | SUBSIZED | SIZED;
            }
        };
    }
}
