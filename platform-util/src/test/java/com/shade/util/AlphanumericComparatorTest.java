package com.shade.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

public class AlphanumericComparatorTest {
    @ParameterizedTest
    @MethodSource("getInput")
    public void alphanumericComparatorTest(List<String> input, List<String> expected) {
        List<String> actual = input.stream()
            .sorted(AlphanumericComparator.getInstance())
            .toList();

        Assertions.assertEquals(expected, actual);
    }

    private static Stream<Arguments> getInput() {
        return Stream.of(
            Arguments.of(
                List.of("img12", "img10", "img2", "img1", "img100_1", "img100_11", "img100", "img100_10", "img100_5"),
                List.of("img1", "img2", "img10", "img12", "img100", "img100_1", "img100_5", "img100_10", "img100_11")
            ),
            Arguments.of(
                List.of("file1000", "file100", "file10", "file2", "file1", "file11", "file200", "file20", "file2a", "file2b"),
                List.of("file1", "file2", "file2a", "file2b", "file10", "file11", "file20", "file100", "file200", "file1000")
            ),
            Arguments.of(
                List.of("doc10", "doc2", "doc20", "doc1", "doc3", "doc15", "doc4"),
                List.of("doc1", "doc2", "doc3", "doc4", "doc10", "doc15", "doc20")
            ),
            Arguments.of(
                List.of("page2", "page12", "page3", "page22", "page4", "page13", "page23"),
                List.of("page2", "page3", "page4", "page12", "page13", "page22", "page23")
            ),
            Arguments.of(
                List.of("item-5", "item-1", "item-10", "item-2", "item-11", "item-3", "item-20", "item-15"),
                List.of("item-1", "item-2", "item-3", "item-5", "item-10", "item-11", "item-15", "item-20")
            ),
            Arguments.of(
                List.of("abc123def45", "abc12def345", "abc1234def5", "abc12345def4", "abc1234def45", "abc12345def456"),
                List.of("abc12def345", "abc123def45", "abc1234def5", "abc1234def45", "abc12345def4", "abc12345def456")
            ),
            Arguments.of(
                List.of("file01_v2", "file02_v1", "file10_v10", "file2_v10", "file10_v2", "file1_v10"),
                List.of("file01_v2", "file1_v10", "file02_v1", "file2_v10", "file10_v2", "file10_v10")
            ),
            Arguments.of(
                List.of("chapter-2.1.1", "chapter-10.1.1", "chapter-2.1.10", "chapter-10.1.2", "chapter-2.1.2"),
                List.of("chapter-2.1.1", "chapter-2.1.2", "chapter-2.1.10", "chapter-10.1.1", "chapter-10.1.2")
            ),
            Arguments.of(
                List.of("item_1_2_3", "item_2_1_3", "item_1_10_3", "item_2_2_1", "item_1_2_10", "item_2_1_10", "item_1_10_1"),
                List.of("item_1_2_3", "item_1_2_10", "item_1_10_1", "item_1_10_3", "item_2_1_3", "item_2_1_10", "item_2_2_1")
            ),
            Arguments.of(
                List.of("1a18c51b-a014-ee37-6bc4-9fa54f49ec0e", "01e6aeeb-6b15-39a5-1791-eb084b56eace", "1fcbd975-45c8-0cd3-df83-c3ad38ac3ee8"),
                List.of("01e6aeeb-6b15-39a5-1791-eb084b56eace", "1a18c51b-a014-ee37-6bc4-9fa54f49ec0e", "1fcbd975-45c8-0cd3-df83-c3ad38ac3ee8")
            ),
            Arguments.of(
                List.of("100", "10", "1", "0", "01", "001"),
                List.of("0", "001", "01", "1", "10", "100")
            )
        );
    }
}
