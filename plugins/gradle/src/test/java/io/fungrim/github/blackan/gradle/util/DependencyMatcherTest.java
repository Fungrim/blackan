package io.fungrim.github.blackan.gradle.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DependencyMatcherTest {

    @Nested
    class MatchesWildcard {

        @Test
        void exactMatch() {
            assertTrue(DependencyMatcher.matchesWildcard("org.slf4j:slf4j-api", "org.slf4j:slf4j-api"));
        }

        @Test
        void noMatch() {
            assertFalse(DependencyMatcher.matchesWildcard("org.slf4j:slf4j-api", "com.google:guava"));
        }

        @Test
        void wildcardArtifact() {
            assertTrue(DependencyMatcher.matchesWildcard("org.slf4j:slf4j-api", "org.slf4j:*"));
        }

        @Test
        void wildcardGroup() {
            assertTrue(DependencyMatcher.matchesWildcard("org.slf4j:slf4j-api", "*:slf4j-api"));
        }

        @Test
        void doubleWildcard() {
            assertTrue(DependencyMatcher.matchesWildcard("org.slf4j:slf4j-api", "*:*"));
        }

        @Test
        void partialWildcardInGroup() {
            assertTrue(DependencyMatcher.matchesWildcard("io.github.fungrim.blackan:common", "io.github.fungrim.*:*"));
        }

        @Test
        void partialWildcardInArtifact() {
            assertTrue(DependencyMatcher.matchesWildcard("org.slf4j:slf4j-api", "org.slf4j:slf4j-*"));
        }

        @Test
        void wildcardDoesNotMatchPartialGroup() {
            assertFalse(DependencyMatcher.matchesWildcard("com.example:lib", "org.*:*"));
        }

        @Test
        void dotsInGroupAreNotTreatedAsRegexDots() {
            assertFalse(DependencyMatcher.matchesWildcard("orgXslf4j:api", "org.slf4j:*"));
        }
    }

    @Nested
    class ShouldIndex {

        @Test
        void indexAllIncludesEverything() {
            assertTrue(DependencyMatcher.shouldIndex("org.slf4j:slf4j-api", true, List.of(), List.of()));
        }

        @Test
        void indexAllRespectsExcludes() {
            assertFalse(DependencyMatcher.shouldIndex("org.slf4j:slf4j-api", true, List.of(), List.of("org.slf4j:*")));
        }

        @Test
        void includeMatchesSpecificDependency() {
            assertTrue(DependencyMatcher.shouldIndex(
                    "io.github.fungrim:common", false,
                    List.of("io.github.fungrim:*"), List.of()));
        }

        @Test
        void excludeTakesPriorityOverInclude() {
            assertFalse(DependencyMatcher.shouldIndex(
                    "org.slf4j:slf4j-api", false,
                    List.of("*:*"), List.of("org.slf4j:*")));
        }

        @Test
        void notIndexAllAndNoIncludesReturnsFalse() {
            assertFalse(DependencyMatcher.shouldIndex("org.slf4j:slf4j-api", false, List.of(), List.of()));
        }

        @Test
        void notIndexAllAndNonMatchingIncludeReturnsFalse() {
            assertFalse(DependencyMatcher.shouldIndex(
                    "org.slf4j:slf4j-api", false,
                    List.of("com.google:*"), List.of()));
        }

        @Test
        void multipleIncludesMatchSecond() {
            assertTrue(DependencyMatcher.shouldIndex(
                    "org.slf4j:slf4j-api", false,
                    List.of("com.google:*", "org.slf4j:*"), List.of()));
        }

        @Test
        void multipleExcludesMatchFirst() {
            assertFalse(DependencyMatcher.shouldIndex(
                    "org.slf4j:slf4j-api", true,
                    List.of(), List.of("org.slf4j:*", "com.google:*")));
        }

        @Test
        void excludeDoesNotAffectNonMatchingCoordinate() {
            assertTrue(DependencyMatcher.shouldIndex(
                    "com.google:guava", true,
                    List.of(), List.of("org.slf4j:*")));
        }
    }

    @Nested
    class ExtractCoordinate {

        @Test
        void mavenLocalRepository() {
            // ~/.m2/repository/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar
            File jar = new File(
                    String.join(File.separator, "", "home", "user", ".m2", "repository",
                            "org", "slf4j", "slf4j-api", "2.0.17", "slf4j-api-2.0.17.jar"));
            assertEquals("org.slf4j:slf4j-api", DependencyMatcher.extractCoordinate(jar));
        }

        @Test
        void mavenLocalNestedGroup() {
            // ~/.m2/repository/io/github/fungrim/blackan/common/0.0.1/common-0.0.1.jar
            File jar = new File(
                    String.join(File.separator, "", "home", "user", ".m2", "repository",
                            "io", "github", "fungrim", "blackan", "common", "0.0.1", "common-0.0.1.jar"));
            assertEquals("io.github.fungrim.blackan:common", DependencyMatcher.extractCoordinate(jar));
        }

        @Test
        void gradleCacheRepository() {
            // ~/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/2.0.17/<hash>/slf4j-api-2.0.17.jar
            File jar = new File(
                    String.join(File.separator, "", "home", "user", ".gradle", "caches",
                            "modules-2", "files-2.1", "org.slf4j", "slf4j-api", "2.0.17",
                            "abc123", "slf4j-api-2.0.17.jar"));
            assertEquals("org.slf4j:slf4j-api", DependencyMatcher.extractCoordinate(jar));
        }

        @Test
        void returnsNullForFileWithNoParent() {
            File jar = new File("standalone.jar");
            assertNull(DependencyMatcher.extractCoordinate(jar));
        }

        @Test
        void returnsNullForFileWithOnlyOneParent() {
            File jar = new File(File.separator + "standalone.jar");
            assertNull(DependencyMatcher.extractCoordinate(jar));
        }

        @Test
        void returnsNullForUnrecognizedLayout() {
            File jar = new File(
                    String.join(File.separator, "", "tmp", "libs", "some-lib", "1.0", "some-lib-1.0.jar"));
            assertNull(DependencyMatcher.extractCoordinate(jar));
        }

        @Test
        void mavenLocalSingleSegmentGroup() {
            // ~/.m2/repository/junit/junit/4.13/junit-4.13.jar
            File jar = new File(
                    String.join(File.separator, "", "home", "user", ".m2", "repository",
                            "junit", "junit", "4.13", "junit-4.13.jar"));
            assertEquals("junit:junit", DependencyMatcher.extractCoordinate(jar));
        }
    }
}
