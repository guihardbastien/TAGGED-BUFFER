# Java tp noté 

http://www-igm.univ-mlv.fr/ens/IR/IR2/2020-2021/JavaAvance/exam.php

## Q1 
```java
package fr.guihardbastien.boilerplate;

import java.util.Objects;
import java.util.function.Predicate;

public class TaggedBuffer<T> {
    private int arraySize = 4;
    private int eltSize = 0;
    private int filteredEltSize = 0;

    private Predicate<? super T> predicate;
    private T[] elements = (T[]) new Object[arraySize];

    public TaggedBuffer(Predicate<? super T> fun) {
        Objects.requireNonNull(fun);
        this.predicate = fun;
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
}
```
## Q2

```java
        if (eltSize == arraySize) {
            this.arraySize *= 2;
            this.elements = Arrays.copyOf(this.elements, this.arraySize);
        }
```

## Q3

```java
    public Optional<T> findFirst(boolean onlyTagged) {
        if (onlyTagged) {
            return Optional.ofNullable(filteredElements[0]);
        } else {
            return Optional.ofNullable(elements[0]);
        }
    }
```

## Q4

```java
    public void forEach(boolean onlyTagged, Consumer<? super T> fun) {
        if (onlyTagged){
            Arrays.stream(filteredElements).filter(Objects::nonNull).forEach(fun);
        } else {
            Arrays.stream(elements).filter(Objects::nonNull).forEach(fun);
        }
    }
```

## Q5
```java

    public Iterator<T> iterator(boolean onlyTagged) {
        return new Iterator<T>() {

            private int index = 0;
            private List<T> filteredEltVue = Arrays.asList(TaggedBuffer.this.filteredElements);
            private List<T> eltVue = Arrays.asList(TaggedBuffer.this.elements);
            private int vueEltSize = TaggedBuffer.this.eltSize;
            private int vueFilteredEltSize = TaggedBuffer.this.filteredEltSize;

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

/*
    public Iterator<T> iterator(boolean onlyTagged) {
        return new Iterator<T>() {

            private int index = 0;

            public boolean hasNext() {
                return index < TaggedBuffer.this.size(onlyTagged);
            }

            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T elt;
                if (onlyTagged){
                    elt = TaggedBuffer.this.filteredElements[index];
                } else {
                    elt = TaggedBuffer.this.elements[index];
                }

                index++;
                return elt;
            }
        };
    }
*/
```

## flashcard
`Objects.checkIndex(row, size()); // checks if index is in range 0 < x < size()`

## Q6

```java
    public List<T> asTaggedList() {
        class TaggedList extends AbstractList<T> implements RandomAccess {
            private int[] filteredIndexes = TaggedBuffer.this.filteredEltIndexes;
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

```