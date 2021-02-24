package fr.guihardbastien.boilerplate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;

public class TaggedBufferTest {

    @Nested
    class Q1 {
        @Test
        @Tag("Q1")
        public void addAndSize() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            buffer.add(1);
            buffer.add(2);
            buffer.add(3);
            buffer.add(5);
            assertEquals(1, buffer.size(true));
            assertEquals(4, buffer.size(false));
        }

        @Test
        @Tag("Q1")
        public void addAndSize2() {
            var buffer = new TaggedBuffer<String>(s -> s.charAt(0) == 'b');
            buffer.add("foo");
            buffer.add("bar");
            buffer.add("baz");
            assertEquals(2, buffer.size(true));
            assertEquals(3, buffer.size(false));
        }

        @Test
        @Tag("Q1")
        public void addTwiceTheSame() {
            var buffer = new TaggedBuffer<String>(s -> s.charAt(0) == 'b');
            buffer.add("foo");
            buffer.add("bar");
            buffer.add("bar");
            assertEquals(2, buffer.size(true));
            assertEquals(3, buffer.size(false));
        }

        @Test
        @Tag("Q1")
        public void emptySize() {
            var buffer = new TaggedBuffer<URI>(s -> fail());
            assertEquals(0, buffer.size(true));
            assertEquals(0, buffer.size(false));
        }

        @Test
        @Tag("Q1")
        public void allTagged() {
            var buffer = new TaggedBuffer<String>(s -> true);
            buffer.add("foo");
            buffer.add("bar");
            buffer.add("baz");
            buffer.add("whizz");
            assertEquals(4, buffer.size(true));
            assertEquals(4, buffer.size(false));
        }

        @Test
        @Tag("Q1")
        public void noTag() {
            var buffer = new TaggedBuffer<String>(s -> false);
            buffer.add("foo");
            buffer.add("bar");
            buffer.add("baz");
            buffer.add("whizz");
            assertEquals(0, buffer.size(true));
            assertEquals(4, buffer.size(false));
        }

        @Test
        @Tag("Q1")
        public void taggedBufferSignature() {
            var buffer = new TaggedBuffer<String>((Object o) -> o.toString().startsWith("ba"));
            buffer.add("foo");
            buffer.add("bar");
            buffer.add("baz");
            assertEquals(2, buffer.size(true));
            assertEquals(3, buffer.size(false));
        }

        @Test
        @Tag("Q1")
        public void sizeInConstantTime() {
            var box = new Object() {
                boolean fail;
            };
            var buffer = new TaggedBuffer<Boolean>(b -> {
                if (box.fail) {
                    fail();
                }
                return b;
            });
            buffer.add(true);
            buffer.add(false);
            buffer.add(true);
            buffer.add(true);
            box.fail = true;
            assertEquals(3, buffer.size(true));
            assertEquals(4, buffer.size(false));
        }

        @Test
        @Tag("Q1")
        public void gapBufferPreconditions() {
            assertAll(
                    () -> assertThrows(NullPointerException.class, () -> new TaggedBuffer<>(null)),
                    () -> assertThrows(NullPointerException.class, () -> new TaggedBuffer<>(__ -> true).add(null))
            );
        }
    }

    @Nested
    class Q2 {

        @Test
        @Tag("Q2")
        public void addAndSizeALot() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            range(0, 1_000_000).forEach(buffer::add);
            assertEquals(1_000_000 / 2, buffer.size(true));
            assertEquals(1_000_000, buffer.size(false));
        }
    }

    @Nested
    class Q3 {

        @Test
        @Tag("Q3")
        public void findFirstWithStrings() {
            var buffer = new TaggedBuffer<String>(s -> s.charAt(0) != 'b');
            buffer.add("booz");
            buffer.add("bar");
            buffer.add("foo");
            buffer.add("baz");
            buffer.add("whizz");
            assertAll(
                    () -> assertTrue(buffer.findFirst(true).isPresent()),
                    () -> assertEquals("foo", buffer.findFirst(true).orElseThrow()),
                    () -> assertTrue(buffer.findFirst(false).isPresent()),
                    () -> assertEquals("booz", buffer.findFirst(false).orElseThrow())
            );
        }

        @Test
        @Tag("Q3")
        public void findFirstWithIntegers() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            buffer.add(1);
            buffer.add(3);
            buffer.add(2);
            assertAll(
                    () -> assertTrue(buffer.findFirst(true).isPresent()),
                    () -> assertEquals(2, buffer.findFirst(true).orElseThrow()),
                    () -> assertTrue(buffer.findFirst(false).isPresent()),
                    () -> assertEquals(1, buffer.findFirst(false).orElseThrow())
            );
        }

        @Test
        @Tag("Q3")
        public void findFirstEmpty() {
            var buffer = new TaggedBuffer<Boolean>(b -> fail());
            assertAll(
                    () -> assertTrue(buffer.findFirst(true).isEmpty()),
                    () -> assertTrue(buffer.findFirst(false).isEmpty())
            );
        }

        @Test
        @Tag("Q3")
        public void findAllTagged() {
            var buffer = new TaggedBuffer<String>(__ -> false);
            buffer.add("foo");
            buffer.add("bar");
            assertAll(
                    () -> assertTrue(buffer.findFirst(true).isEmpty()),
                    () -> assertEquals("foo", buffer.findFirst(false).orElseThrow())
            );
        }

        @Test
        @Tag("Q3")
        public void findFirstNoTagDoesNotCallFilter() {
            var box = new Object() {
                boolean fail;
            };
            var buffer = new TaggedBuffer<Integer>(i -> {
                if (box.fail) {
                    fail();
                }
                return i < 5;
            });
            buffer.add(3);
            buffer.add(13);
            box.fail = true;
            assertAll(
                    () -> assertTrue(buffer.findFirst(false).isPresent()),
                    () -> assertEquals(3, buffer.findFirst(false).orElseThrow())
            );
        }
    }

    @Nested
    class Q4 {

        @Test
        @Tag("Q4")
        public void forEachTag() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            buffer.add(1);
            buffer.add(2);
            buffer.add(3);
            buffer.add(5);
            var list = new ArrayList<Integer>();
            buffer.forEach(true, list::add);
            assertEquals(List.of(1, 3, 5), list);
        }

        @Test
        @Tag("Q4")
        public void forEachNoTag() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            buffer.add(3);
            buffer.add(7);
            buffer.add(8);
            buffer.add(5);
            var list = new ArrayList<Integer>();
            buffer.forEach(false, list::add);
            assertEquals(List.of(3, 7, 8, 5), list);
        }

        @Test
        @Tag("Q4")
        public void forEachSignature() {
            var buffer = new TaggedBuffer<Integer>((Object o) -> false);
            buffer.add(23);
            buffer.add(70);
            buffer.add(56);
            var list = new ArrayList<Object>();
            buffer.forEach(true, list::add);
            assertEquals(List.of(), list);
        }

        @Test
        @Tag("Q4")
        public void forEachALot() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            range(0, 10_000).forEach(buffer::add);
            var list = new ArrayList<Integer>();
            buffer.forEach(false, list::add);
            assertEquals(range(0, 10_000).boxed().collect(toList()), list);
        }

        @Test
        @Tag("Q4")
        public void forEachALot2() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            range(0, 10_000).forEach(buffer::add);
            var list = new ArrayList<Integer>();
            buffer.forEach(true, list::add);
            assertEquals(range(0, 10_000).filter(i -> i % 2 == 1).boxed().collect(toList()), list);
        }

        @Test
        @Tag("Q4")
        public void forEachNoTagDoesNotCallFilter() {
            var box = new Object() {
                boolean fail;
            };
            var buffer = new TaggedBuffer<Integer>(i -> {
                if (box.fail) {
                    fail();
                }
                return i < 10;
            });
            buffer.add(3);
            buffer.add(13);
            buffer.add(2);
            box.fail = true;
            var list = new ArrayList<Integer>();
            buffer.forEach(false, list::add);
            assertEquals(List.of(3, 13, 2), list);
        }

        @Test
        @Tag("Q4")
        public void forEachPrecondition() {
            assertThrows(NullPointerException.class, () -> new TaggedBuffer<>(null));
        }
    }


    @Nested
    class Q5 {

        @Test
        @Tag("Q5")
        public void iteratorTaggedWithIntegers() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            buffer.add(11);
            buffer.add(12);
            buffer.add(13);
            buffer.add(15);
            var list = new ArrayList<Integer>();
            buffer.iterator(true).forEachRemaining(list::add);
            assertEquals(List.of(11, 13, 15), list);
        }

        @Test
        @Tag("Q5")
        public void iteratorTaggedWithStrings() {
            var buffer = new TaggedBuffer<String>(s -> s.charAt(0) != 'b');
            buffer.add("foo");
            buffer.add("bar");
            buffer.add("baz");
            var list = new ArrayList<String>();
            buffer.iterator(true).forEachRemaining(list::add);
            assertEquals(List.of("foo"), list);
        }

        @Test
        @Tag("Q5")
        public void iteratorNoTag() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            buffer.add(101);
            buffer.add(102);
            buffer.add(103);
            buffer.add(105);
            var list = new ArrayList<Integer>();
            buffer.iterator(false).forEachRemaining(list::add);
            assertEquals(List.of(101, 102, 103, 105), list);
        }

        @Test
        @Tag("Q5")
        public void iteratorSATB() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 10 == 0);
            buffer.add(2);
            buffer.add(100);
            var it = buffer.iterator(true);
            buffer.add(200);
            var list = new ArrayList<Integer>();
            it.forEachRemaining(list::add);
            assertEquals(List.of(100), list);
        }

        @Test
        @Tag("Q5")
        public void iteratorALot() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            range(0, 1_000_000).forEach(buffer::add);
            var list = new ArrayList<Integer>();
            assertTimeoutPreemptively(Duration.ofMillis(2_000), () -> buffer.iterator(false).forEachRemaining(list::add));
            assertEquals(range(0, 1_000_000).boxed().collect(toList()), list);
        }

        @Test
        @Tag("Q5")
        public void iteratorALot2() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            range(0, 1_000_000).forEach(buffer::add);
            var list = new ArrayList<Integer>();
            assertTimeoutPreemptively(Duration.ofMillis(2_000), () -> buffer.iterator(true).forEachRemaining(list::add));
            assertEquals(range(0, 1_000_000).filter(i -> i % 2 == 1).boxed().collect(toList()), list);
        }

        @Test
        @Tag("Q5")
        public void iteratorDoNotCallPredicateIfNotNecessary() {
            var box = new Object() {
                boolean shouldFail;
            };
            var buffer = new TaggedBuffer<Integer>(i -> {
                if (i == 6 && box.shouldFail) {
                    fail();
                }
                return i % 2 == 0;
            });
            buffer.add(1);
            buffer.add(2);
            buffer.add(6);
            buffer.add(7);
            box.shouldFail = true;
            var iterator = buffer.iterator(true);
            assertEquals(2, iterator.next());
        }

        @Test
        @Tag("Q5")
        public void iteratorNoRemoveTags() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            buffer.add(3);
            buffer.add(1);
            buffer.add(2);
            var it = buffer.iterator(true);
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove);
        }

        @Test
        @Tag("Q5")
        public void iteratorNoRemoveNoTag() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            buffer.add(3);
            buffer.add(1);
            buffer.add(2);
            var it = buffer.iterator(false);
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove);
        }

        @Test
        @Tag("Q5")
        public void iteratorEmpty() {
            var buffer = new TaggedBuffer<String>(s -> fail());
            assertAll(
                    () -> assertFalse(buffer.iterator(true).hasNext()),
                    () -> assertThrows(NoSuchElementException.class, () -> buffer.iterator(true).next()),
                    () -> assertFalse(buffer.iterator(false).hasNext()),
                    () -> assertThrows(NoSuchElementException.class, () -> buffer.iterator(false).next())
            );
        }

        @Test
        @Tag("Q5")
        public void iteratorOnlyNextTag() {
            var buffer = new TaggedBuffer<Integer>(i -> i >= 10);
            buffer.add(3);
            buffer.add(42);
            buffer.add(7);
            buffer.add(17);
            buffer.add(54);
            var list = new ArrayList<Integer>();
            var it = buffer.iterator(true);
            assertEquals(42, it.next());
            assertEquals(17, it.next());
            assertEquals(54, it.next());
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::next);
        }

        @Test
        @Tag("Q5")
        public void iteratorOnlyNextNoTag() {
            var buffer = new TaggedBuffer<Integer>(i -> i >= 110);
            buffer.add(13);
            buffer.add(142);
            buffer.add(13);
            buffer.add(117);
            buffer.add(154);
            var list = new ArrayList<Integer>();
            var it = buffer.iterator(false);
            assertEquals(13, it.next());
            assertEquals(142, it.next());
            assertEquals(13, it.next());
            assertEquals(117, it.next());
            assertEquals(154, it.next());
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::next);
        }

        @Test
        @Tag("Q5")
        public void iteratorOnlyNextTag2() {
            var buffer = new TaggedBuffer<String>(s -> s.length() == 4);
            buffer.add("foo");
            buffer.add("farr");
            buffer.add("bar");
            var list = new ArrayList<String>();
            var it = buffer.iterator(true);
            assertEquals("farr", it.next());
            assertThrows(NoSuchElementException.class, it::next);
            assertFalse(it.hasNext());
        }

        @Test
        @Tag("Q5")
        public void iteratorOnlyNextNoCallToFilter() {
            var box = new Object() {
                boolean fail;
            };
            var buffer = new TaggedBuffer<String>(s -> {
                if (box.fail) {
                    fail();
                }
                return s.length() == 5;
            });
            buffer.add("baz");
            buffer.add("foo");
            buffer.add("whizz");
            var list = new ArrayList<String>();
            var it = buffer.iterator(false);
            box.fail = true;
            assertEquals("baz", it.next());
            assertEquals("foo", it.next());
            assertEquals("whizz", it.next());
            assertThrows(NoSuchElementException.class, it::next);
            assertFalse(it.hasNext());
        }

        @Test
        @Tag("Q5")
        public void iteratorHasNextNoSideEffect() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 5 == 0);
            buffer.add(1);
            buffer.add(10);
            buffer.add(20);
            buffer.add(3);
            buffer.add(70);
            var it = buffer.iterator(true);
            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
            assertEquals(10, it.next());
            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
            assertEquals(20, it.next());
            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
            assertEquals(70, it.next());
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::next);
        }
    }


    @Nested
    class Q6 {

        @Test
        @Tag("Q6")
        public void asTaggedList() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            buffer.add(12);
            buffer.add(13);
            buffer.add(14);
            assertEquals(List.of(12, 14), buffer.asTaggedList());
        }

        @Test
        @Tag("Q6")
        public void asTaggedListEmpty() {
            var buffer = new TaggedBuffer<String>(s -> fail());
            assertEquals(List.of(), buffer.asTaggedList());
        }

        @Test
        @Tag("Q6")
        public void asTaggedListGetALot() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            range(0, 1_000_000).forEach(buffer::add);
            var i = 0;
            for (var element : buffer.asTaggedList()) {
                assertEquals(i, element);
                i += 2;
            }
        }

        @Test
        @Tag("Q6")
        public void asTaggedListImplementation() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            buffer.add(1);
            buffer.add(2);
            var list = buffer.asTaggedList();
            assertAll(
                    () -> assertThrows(UnsupportedOperationException.class, () -> list.add(3)),
                    () -> assertThrows(UnsupportedOperationException.class, () -> list.addAll(List.of(3))),
                    () -> assertThrows(UnsupportedOperationException.class, () -> list.remove(2)),
                    () -> assertThrows(UnsupportedOperationException.class, () -> list.removeAll(List.of(2))),
                    () -> assertThrows(UnsupportedOperationException.class, list::clear),
                    () -> {
                        var it = list.iterator();
                        it.next();
                        assertThrows(UnsupportedOperationException.class, it::remove);
                    }
            );
        }

        @Test
        @Tag("Q6")
        public void asTaggedListRandomAccess() {
            // Si vous ne voyez pas pour ce test, pas grave passer à la suite !
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            var list = buffer.asTaggedList();
            assertTrue(list instanceof RandomAccess);
        }

        @Test
        @Tag("Q6")
        public void asTaggedListSATB() {
            var buffer = new TaggedBuffer<String>(s -> s.charAt(1) == 'a');
            buffer.add("bar");
            buffer.add("boo");
            buffer.add("wat");
            var list = buffer.asTaggedList();
            buffer.add("fuzz");
            assertEquals(List.of("bar", "wat"), list);
        }

        @Test
        @Tag("Q6")
        public void asTaggedListIterationWithGet() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 10 == 0);
            range(0, 1_000_000).forEach(buffer::add);
            var list = buffer.asTaggedList();
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                var sum = 0L;
                for (var i = 0; i < list.size(); i++) {
                    sum += list.get(i);
                }
                assertEquals(49999500000L, sum);
            });
        }
    }


    @Nested
    class Q7 {

        @Test
        @Tag("Q7")
        public void streamOfIntegers() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            buffer.add(2);
            buffer.add(4);
            buffer.add(5);
            assertEquals(List.of(5), buffer.stream(true).collect(toList()));
            assertEquals(List.of(2, 4, 5), buffer.stream(false).collect(toList()));
        }

        @Test
        @Tag("Q7")
        public void streamOfStrings() {
            var buffer = new TaggedBuffer<String>(s -> s.charAt(0) != 'b');
            buffer.add("foo");
            buffer.add("bar");
            buffer.add("baz");
            assertEquals(List.of("foo"), buffer.stream(true).collect(toList()));
            assertEquals(List.of("foo", "bar", "baz"), buffer.stream(false).collect(toList()));
        }

        @Test
        @Tag("Q7")
        public void streamTagCharacteristics() {
            // Attention, cela ne veut pas pas dire que le Stream à que cette caractéristique !
            var buffer = new TaggedBuffer<String>(s -> s.charAt(0) == 'b');
            var spliterator = buffer.stream(true).spliterator();
            assertTrue(spliterator.hasCharacteristics(Spliterator.NONNULL));
        }

        @Test
        @Tag("Q7")
        public void streamNoTagCharacteristics() {
            // Attention, cela ne veut pas pas dire que le Stream à que cette caractéristique !
            var buffer = new TaggedBuffer<String>(s -> s.charAt(0) == 'b');
            var spliterator = buffer.stream(false).spliterator();
            assertTrue(spliterator.hasCharacteristics(Spliterator.NONNULL));
        }

        @Test
        @Tag("Q7")
        public void streamMethodNoSideEffect() {
            var box = new Object() {
                boolean fail;
            };
            var buffer = new TaggedBuffer<Integer>(i -> {
                if (box.fail) {
                    fail();
                }
                return i % 2 == 1;
            });
            buffer.add(1);
            buffer.add(2);
            buffer.add(3);
            box.fail = true;
            assertAll(
                    () -> buffer.stream(true),
                    () -> buffer.stream(false)
            );
        }

        @Test
        @Tag("Q7")
        public void streamTagSpliteratorEstimateSize() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            buffer.add(0);
            buffer.add(1);
            buffer.add(3);
            var spliterator = buffer.stream(true).spliterator();
            var estimateSize = spliterator.estimateSize();
            spliterator.tryAdvance(__ -> {
            });
            var estimateSize2 = spliterator.estimateSize();
            assertFalse(estimateSize2 == estimateSize);
        }

        @Test
        @Tag("Q7")
        public void streamNoTagSpliteratorEstimateSize() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 0);
            buffer.add(10);
            buffer.add(11);
            buffer.add(13);
            var spliterator = buffer.stream(false).spliterator();
            var estimateSize = spliterator.estimateSize();
            spliterator.tryAdvance(__ -> {
            });
            var estimateSize2 = spliterator.estimateSize();
            assertFalse(estimateSize2 == estimateSize);
        }

        @Test
        @Tag("Q7")
        public void streamTagSATB() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            buffer.add(5);
            buffer.add(10);
            buffer.add(15);
            var stream = buffer.stream(true);
            buffer.add(17);
            assertEquals(List.of(5, 15), stream.collect(toList()));
        }

        @Test
        @Tag("Q7")
        public void streamNoTagSATB() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 2 == 1);
            buffer.add(5);
            buffer.add(10);
            buffer.add(15);
            var stream = buffer.stream(false);
            buffer.add(17);
            assertEquals(List.of(5, 10, 15), stream.collect(toList()));
        }
    }


    @Nested
    class Q8 {

        @Test
        @Tag("Q8")
        public void streamGapsSpliteratorTrySplit() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 100 != 0);
            range(0, 10_000).forEach(buffer::add);
            var spliterator = buffer.stream(true).spliterator();
            var estimateSize = spliterator.estimateSize();
            var spliterator2 = spliterator.trySplit();
            assertNotNull(spliterator2);
            var estimateSize1 = spliterator.estimateSize();
            var estimateSize2 = spliterator2.estimateSize();
            assertAll(
                    () -> assertTrue(estimateSize1 < estimateSize),
                    () -> assertTrue(estimateSize2 < estimateSize)
            );
        }

        @Test
        @Tag("Q8")
        public void streamNoGapsSpliteratorTrySplit() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 100 != 0);
            range(0, 10_000).forEach(buffer::add);
            var spliterator = buffer.stream(false).spliterator();
            var estimateSize = spliterator.estimateSize();
            var spliterator2 = spliterator.trySplit();
            assertNotNull(spliterator2);
            var estimateSize1 = spliterator.estimateSize();
            var estimateSize2 = spliterator2.estimateSize();
            assertAll(
                    () -> assertTrue(estimateSize1 < estimateSize),
                    () -> assertTrue(estimateSize2 < estimateSize)
            );
        }

        @Test
        @Tag("Q8")
        public void streamEmptySpliteratorTrySplit() {
            var buffer = new TaggedBuffer<String>(s -> fail());
            assertAll(
                    () -> assertNull(buffer.stream(true).spliterator().trySplit()),
                    () -> assertNull(buffer.stream(false).spliterator().trySplit())
            );
        }

        @Test
        @Tag("Q8")
        public void streamTagParallelCount() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 100 == 0);
            range(0, 1_000_000).forEach(buffer::add);
            var count = buffer.stream(true).parallel().count();
            assertEquals(10_000, count);
        }

        @Test
        @Tag("Q8")
        public void streamNoTagParallelCount() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 100 == 0);
            range(0, 1_000_000).forEach(buffer::add);
            var count = buffer.stream(false).parallel().count();
            assertEquals(1_000_000, count);
        }

        @Test
        @Tag("Q8")
        public void streamTagParallelSum() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 100 == 0);
            range(0, 1_000_000).forEach(buffer::add);
            var sum = buffer.stream(true).parallel().mapToInt(i -> i).sum();
            assertEquals(704_532_704, sum);
        }

        @Test
        @Tag("Q8")
        public void streamNoTagParallelSum() {
            var buffer = new TaggedBuffer<Integer>(i -> i % 100 == 0);
            range(0, 1_000_000).forEach(buffer::add);
            var sum = buffer.stream(false).parallel().mapToInt(i -> i).sum();
            assertEquals(1_783_293_664, sum);
        }

        @Test
        @Tag("Q8")
        public void streamNoTagCharacteristics2() {
            // Attention, cela ne veut pas pas dire que le Stream à que cette caractéristique !
            var buffer = new TaggedBuffer<String>(s -> s.charAt(0) == 'b');
            var spliterator = buffer.stream(false).spliterator();
            assertTrue(spliterator.hasCharacteristics(Spliterator.SUBSIZED));
        }
    }

}

