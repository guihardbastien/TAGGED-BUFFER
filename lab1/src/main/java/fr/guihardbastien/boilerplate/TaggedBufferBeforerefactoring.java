package fr.guihardbastien.boilerplate;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TaggedBufferBeforerefactoring<T> {
    private int arraySize = 4;
    private int eltSize = 0;
    private int filteredEltSize = 0;
    private Predicate<Object> predicate;
    private T[] filteredElements = (T[]) new Object[arraySize]; //todo refactor to index array ?
    private T[] elements = (T[]) new Object[arraySize];

    public TaggedBufferBeforerefactoring(Predicate<? super T> fun) {
        Objects.requireNonNull(fun);
        this.predicate = (Predicate<Object>) fun;
    }

    /**
     * todo refactor, array.length is O(1) ?
     */
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

    public void add(T elt) {
        Objects.requireNonNull(elt);
        grow();
        elements[eltSize] = elt;
        eltSize++;
        if (this.predicate.test(elt)) {
            filteredElements[filteredEltSize] = elt;
            filteredEltSize++;
        }
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
            private List<T> filteredEltVue = Arrays.asList(TaggedBufferBeforerefactoring.this.filteredElements);
            private List<T> eltVue = Arrays.asList(TaggedBufferBeforerefactoring.this.elements);
            private int vueEltSize = TaggedBufferBeforerefactoring.this.eltSize;
            private int vueFilteredEltSize = TaggedBufferBeforerefactoring.this.filteredEltSize;

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
}
