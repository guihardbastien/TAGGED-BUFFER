package fr.guihardbastien.boilerplate;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TaggedBufferBeforeLazyfication<T> {
    private int arraySize = 4;
    private int eltSize = 0;
    private int filteredEltSize = 0;

    private Predicate<Object> predicate;

    private int[] filteredEltIndexes = new int[0];

    private T[] filteredElements = (T[]) new Object[arraySize];
    private T[] elements = (T[]) new Object[arraySize];

    public TaggedBufferBeforeLazyfication(Predicate<? super T> fun) {
        Objects.requireNonNull(fun);
        this.predicate = (Predicate<Object>) fun;
    }

    private void grow() {
        if (eltSize == arraySize) {
            this.arraySize *= 2;
            this.elements = Arrays.copyOf(this.elements, this.arraySize);
        }
        if (filteredEltSize == filteredElements.length) {
            var newSize = filteredElements.length * 2;
            this.filteredElements = Arrays.copyOf(this.filteredElements, newSize);

        }
    }


    public List<T> asTaggedList() {
        class TaggedList extends AbstractList<T> implements RandomAccess {
            private int[] filteredIndexes = TaggedBufferBeforeLazyfication.this.filteredEltIndexes;

            @Override
            public T get(int index) {
                return TaggedBufferBeforeLazyfication.this.elements[filteredIndexes[index]];
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
            filteredElements[filteredEltSize] = elt;

            this.filteredEltIndexes = Arrays.copyOf(this.filteredEltIndexes, filteredEltSize + 1);
            filteredEltIndexes[filteredEltSize] = eltSize;
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
            return Optional.ofNullable(filteredElements[0]);
        } else {
            return Optional.ofNullable(elements[0]);
        }
    }

    public void forEach(boolean onlyTagged, Consumer<? super T> fun) {
        if (onlyTagged) {
            Arrays.stream(filteredElements).filter(Objects::nonNull).forEach(fun);
        } else {
            Arrays.stream(elements).filter(Objects::nonNull).forEach(fun);
        }
    }

    public Iterator<T> iterator(boolean onlyTagged) {
        return new Iterator<T>() {

            private int index = 0;
            private List<T> filteredEltVue = Arrays.asList(TaggedBufferBeforeLazyfication.this.filteredElements);
            private List<T> eltVue = Arrays.asList(TaggedBufferBeforeLazyfication.this.elements);
            private int vueEltSize = TaggedBufferBeforeLazyfication.this.eltSize;
            private int vueFilteredEltSize = TaggedBufferBeforeLazyfication.this.filteredEltSize;

            public boolean hasNext() {
                if (onlyTagged) {
                    return index < vueFilteredEltSize;
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
                    elt = filteredEltVue.get(index);
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

            private Spliterator<T> split = Arrays.asList(onlyTagged ? TaggedBufferBeforeLazyfication.this.filteredElements : TaggedBufferBeforeLazyfication.this.elements)
                    .subList(0, onlyTagged ? filteredEltSize : eltSize)
                    .stream()
                    .filter(Objects::nonNull)
                    .spliterator();

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
                return Spliterator.ORDERED | Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE;
            }
        };
    }

    public Stream<T> stream(boolean onlyTagged) {
        return StreamSupport.stream(createSpliterator(onlyTagged), true);
    }
}
