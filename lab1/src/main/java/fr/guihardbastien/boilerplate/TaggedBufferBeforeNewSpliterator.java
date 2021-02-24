package fr.guihardbastien.boilerplate;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TaggedBufferBeforeNewSpliterator<T> {
    private int arraySize = 4;
    private int eltSize = 0;
    private int filteredEltSize = 0;

    private Predicate<? super T> predicate;
    private T[] elements = (T[]) new Object[arraySize];

    public TaggedBufferBeforeNewSpliterator(Predicate<? super T> fun) {
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
            private Integer[] filteredIndexes = finalIndexes;

            @Override
            public T get(int index) {
                return TaggedBufferBeforeNewSpliterator.this.elements[filteredIndexes[index]];
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
        return;
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

            private List<T> eltVue = Arrays.asList(TaggedBufferBeforeNewSpliterator.this.elements);
            private int vueEltSize = TaggedBufferBeforeNewSpliterator.this.eltSize;
            private int vueFilteredEltSize = TaggedBufferBeforeNewSpliterator.this.filteredEltSize;

            public boolean hasNext() {
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
                    if (!TaggedBufferBeforeNewSpliterator.this.predicate.test(elt)){
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

    private Spliterator<T> createSpliterator(boolean onlyTagged) {
        return new Spliterator<T>() {

            private Spliterator<T> splitElts = Arrays.asList(TaggedBufferBeforeNewSpliterator.this.elements)
                    .subList(0, eltSize)
                    .spliterator();

            private Spliterator<T> splitTaggedList = TaggedBufferBeforeNewSpliterator.this.asTaggedList().spliterator();

            private Spliterator<T> split = onlyTagged ? splitTaggedList : splitElts;

            public boolean tryAdvance(Consumer<? super T> action) {
                return split.tryAdvance(action);
            }

            public Spliterator<T> trySplit() {
                return split.trySplit();
            }

            public long estimateSize() {
                return split.estimateSize();
            }

            public int characteristics() {
                return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE;
            }
        };
    }

    public Stream<T> stream(boolean onlyTagged) {
        return StreamSupport.stream(createSpliterator(onlyTagged), true);
    }
}
