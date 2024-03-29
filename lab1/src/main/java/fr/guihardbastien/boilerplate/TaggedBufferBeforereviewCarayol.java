package fr.guihardbastien.boilerplate;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TaggedBufferBeforereviewCarayol<T> {
    private int arraySize = 4;
    private int eltSize = 0;
    private int filteredEltSize = 0;

    private final Predicate<? super T> predicate;
    private T[] elements = (T[]) new Object[arraySize];

    public TaggedBufferBeforereviewCarayol(Predicate<? super T> fun) {
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
        var indexes = new ArrayList<Integer>();
        for (int i = 0; i < eltSize; i++) {
            if (predicate.test(elements[i])) {
                indexes.add(i);
            }
        }

        var finalIndexes = new Integer[filteredEltSize];
        indexes.toArray(finalIndexes);

        class TaggedList extends AbstractList<T> implements RandomAccess {
            private final Integer[] filteredIndexes = finalIndexes;

            @Override
            public T get(int index) {
                return TaggedBufferBeforereviewCarayol.this.elements[filteredIndexes[index]];
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
            Arrays.stream(elements).filter(Objects::nonNull).filter(predicate).forEach(fun);
        } else {
            Arrays.stream(elements).filter(Objects::nonNull).forEach(fun);
        }
    }

    public Iterator<T> iterator(boolean onlyTagged) {
        return new Iterator<T>() {

            private int index = 0;
            private int filteredIndex = 0;

            private final List<T> eltVue = Arrays.asList(TaggedBufferBeforereviewCarayol.this.elements);
            //vue donc ok mais on veut que l'iterateur stock en local end value
            private final int vueEltSize = TaggedBufferBeforereviewCarayol.this.eltSize;
            private final int vueFilteredEltSize = TaggedBufferBeforereviewCarayol.this.filteredEltSize;

            public boolean hasNext() { //faux
                if (onlyTagged) {
                    return filteredIndex < vueFilteredEltSize;
                } else {
                    return index < vueEltSize;
                }
            }

            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T elt;
                if (onlyTagged) {
                    elt = eltVue.get(index);
                    if (!TaggedBufferBeforereviewCarayol.this.predicate.test(elt)){
                        index++;
                        return next();
                    } else {
                        filteredIndex++;
                    }
                } else {
                    elt = eltVue.get(index);
                }
                index++;
                return elt;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public Stream<T> stream(boolean onlyTagged) {
        T[] array;
        array = (T[]) Arrays.stream(this.elements)
                .filter(Objects::nonNull)
                .toArray();

        return StreamSupport.stream(createSpliterator(0, array.length, array, onlyTagged), true);
    }

    private Spliterator<T> createSpliterator(int start, int end, T[] array, boolean onlyTagged) {
        var onlyTaggedSpliterator = new Spliterator<T>() {

            private int i = start;

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                if (i < end) {
                    var tmp = array[i++];
                    if(TaggedBufferBeforereviewCarayol.this.predicate.test(tmp)){
                        consumer.accept(tmp);
                        return true;
                    }
                    return tryAdvance(consumer);
                }
                return false;
            }

            @Override
            public Spliterator<T> trySplit() {
                var middle = (i + end) >>> 1;
                if (middle == i) {
                    return null;
                }
                var spliterator = createSpliterator(i, middle, array, true);
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

        var regularSpliterator = new Spliterator<T>() {
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
                var middle = (i + end) >>> 1;
                if (middle == i) {
                    return null;
                }
                var spliterator = createSpliterator(i, middle, array, false);
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

        return onlyTagged ? onlyTaggedSpliterator : regularSpliterator;
    }
}
