package de.bwaldvogel.mongo.backend;

import static de.bwaldvogel.mongo.TestUtils.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

import de.bwaldvogel.mongo.bson.Document;
import de.bwaldvogel.mongo.exception.MongoServerError;

class ProjectionTest {

    @Test
    void testProjectDocument() throws Exception {
        assertThat(projectDocument(null, new Document(), null)).isNull();

        assertThat(projectDocument(json("_id: 100"), json("_id: 1"), "_id"))
            .isEqualTo(json("_id: 100"));

        assertThat(projectDocument(json("_id: 100, foo: 123"), json("foo: 1"), "_id"))
            .isEqualTo(json("_id: 100, foo: 123"));

        assertThat(projectDocument(json("_id: 100, foo: 123"), json("_id: 1"), "_id"))
            .isEqualTo(json("_id: 100"));

        assertThat(projectDocument(json("_id: 100, foo: 123"), json("_id: 0"), "_id"))
            .isEqualTo(json("foo: 123"));

        assertThat(projectDocument(json("_id: 100, foo: 123"), json("_id: 0, foo: 1"), "_id"))
            .isEqualTo(json("foo: 123"));

        assertThat(projectDocument(json("_id: 100, foo: 123, bar: 456"), json("_id: 0, foo: 1"), "_id"))
            .isEqualTo(json("foo: 123"));

        assertThat(projectDocument(json("_id: 100, foo: 123, bar: 456"), json("foo: 0"), "_id"))
            .isEqualTo(json("_id: 100, bar: 456"));

        assertThat(projectDocument(json("_id: 1, foo: {bar: 123, bla: 'x'}"), json("'foo.bar': 1"), "_id"))
            .isEqualTo(json("_id: 1, foo: {bar: 123}"));

        assertThat(projectDocument(json("_id: 1"), json("'foo.bar': 1"), "_id"))
            .isEqualTo(json("_id: 1"));

        assertThat(projectDocument(json("_id: 1, foo: {a: 'x', b: 'y', c: 'z'}"), json("'foo.a': 1, 'foo.c': 1"), "_id"))
            .isEqualTo(json("_id: 1, foo: {a: 'x', c: 'z'}"));

        assertThat(projectDocument(json("_id: 1, foo: {a: 'x', b: 'y', c: 'z'}"), json("'foo.b': 0"), "_id"))
            .isEqualTo(json("_id: 1, foo: {a: 'x', c: 'z'}"));
    }

    @Test
    void testInvalidMixedProjections() throws Exception {
        assertThatExceptionOfType(MongoServerError.class)
            .isThrownBy(() -> projectDocument(json("{}"), json("_id: 1, foo: 0"), "_id"))
            .withMessage("[Error 31253] Cannot do inclusion on field _id in exclusion projection");

        assertThatExceptionOfType(MongoServerError.class)
            .isThrownBy(() -> projectDocument(json("{}"), json("_id: 1, foo: 1, bar: 0"), "_id"))
            .withMessage("[Error 31253] Cannot do inclusion on field _id in exclusion projection");

        assertThatExceptionOfType(MongoServerError.class)
            .isThrownBy(() -> projectDocument(json("{}"), json("_id: 1, 'foo.a': 1, 'foo.b': 0"), "_id"))
            .withMessage("[Error 31253] Cannot do inclusion on field _id in exclusion projection");
    }

    @Test
    void testProjectIn() {
        Document document = json("_id: 1, students: [" +
            "{name: 'john', school: 'A', age: 10}, " +
            "{name: 'jess', school: 'B', age: 12}, " +
            "{name: 'jeff', school: 'A', age: 12}" +
            "]");

        assertThat(projectDocument(document, json("_id: 1, \"students.name\": 1")))
            .isEqualTo(json("_id:1, students: [{name: 'john'}, {name: 'jess'}, {name: 'jeff'}]"));

        Document document2 = json("_id: 1, students: {name: 'john', school: 'A', age: 10}");

        assertThat(projectDocument(document2, json("_id: 1, \"students.name\": 1")))
            .isEqualTo(json("_id:1, students: {name: 'john'}"));
    }

    @Test
    void testProjectOut() {
        Document document = json("_id: 1, students: [" +
            "{name: 'john', school: 'A', age: 10}, " +
            "{name: 'jess', school: 'B', age: 12}, " +
            "{name: 'jeff', school: 'A', age: 12}" +
            "]");

        assertThat(projectDocument(document, json("\"students.school\": 0,  \"students.age\": 0")))
            .isEqualTo(json("_id:1, students: [{name: 'john'}, {name: 'jess'}, {name: 'jeff'}]"));

        Document document2 = json("_id: 1, students: {name: 'john', school: 'A', age: 10}");

        assertThat(projectDocument(document2, json("\"students.school\": 0,  \"students.age\": 0")))
            .isEqualTo(json("_id:1, students: {name: 'john'}"));

    }


    @Test
    void testProjectMissingValue() throws Exception {
        assertThat(projectDocument(json("_id: 1"), json("'a.b': 1"), "_id"))
            .isEqualTo(json("_id: 1"));

        assertThat(projectDocument(json("_id: 1, a: null"), json("'a.b': 1"), "_id"))
            .isEqualTo(json("_id: 1"));

        assertThat(projectDocument(json("_id: 1, a: {b: null}"), json("'a.b': 1"), "_id"))
            .isEqualTo(json("_id: 1, a: {b: null}"));

        assertThat(projectDocument(json("_id: 1, a: {b: null}"), json("'a.c': 1"), "_id"))
            .isEqualTo(json("_id: 1, a: {}"));
    }

    @Test
    void testProjectListValues() throws Exception {
        assertThat(projectDocument(json("_id: 1, a: [1, 2, 3]"), json("'a.c': 1"), "_id"))
            .isEqualTo(json("_id: 1, a: []"));

        assertThat(projectDocument(json("_id: 1, a: [{x: 1}, 500, {y: 2}, {x: 3}]"), json("'a.x': 1"), "_id"))
            .isEqualTo(json("_id: 1, a: [{x: 1}, {}, {x: 3}]"));

        assertThat(projectDocument(json("_id: 1, a: [{x: 10, y: 100}, 100, {x: 20, y: 200}, {x: 3}, {z: 4}]"), json("'a.x': 1, 'a.y': 1"), "_id"))
            .isEqualTo(json("_id: 1, a: [{x: 10, y: 100}, {x: 20, y: 200}, {x: 3}, {}]"));

        assertThat(projectDocument(json("_id: 1, a: [{x: 10, y: 100}, 100, {x: 20, y: 200}, {x: 3}, {z: 4}]"), json("'a.z': 0"), "_id"))
            .isEqualTo(json("_id: 1, a: [{x: 10, y: 100}, 100, {x: 20, y: 200}, {x: 3}, {}]"));

        assertThat(projectDocument(json("_id: 1, a: [100, {z: 4}, {y: 3}, {x: 1, y: 4}]"), json("'a.x': 1, 'a.y': 1"), "_id"))
            .isEqualTo(json("_id: 1, a: [{}, {y: 3}, {x: 1, y: 4}]"));

        assertThat(projectDocument(json("_id: 1, a: [100, {z: 4}, {y: 3}, {x: 1, y: 4}]"), json("'a.z': 0"), "_id"))
            .isEqualTo(json("_id: 1, a: [100, {}, {y: 3}, {x: 1, y: 4}]"));

        assertThat(projectDocument(json("_id: 1, a: [100, {z: 4}, {y: 3}, {x: 1, y: 4}]"), json("'a.x': 0, 'a.z': 0"), "_id"))
            .isEqualTo(json("_id: 1, a: [100, {}, {y: 3}, {y: 4}]"));
    }

    @Test
    void testProjectListValuesWithPositionalOperator() throws Exception {
        Document document = json("_id: 1, students: [" +
            "{name: 'john', school: 'A', age: 10}, " +
            "{name: 'jess', school: 'B', age: 12}, " +
            "{name: 'jeff', school: 'A', age: 12}" +
            "]");

        assertThat(projectDocument(document, json("'students.$': 1"), "_id"))
            .isEqualTo(json("_id: 1, students: [{name: 'john', school: 'A', age: 10}]"));

        assertThat(projectDocument(document, json("'unknown.$': 1"), "_id"))
            .isEqualTo(json("_id: 1"));
    }

    @Test
    void testProjectWithElemMatch() throws Exception {
        Document document = json("_id: 1, students: [" +
            "{name: 'john', school: 'A', age: 10}, " +
            "{name: 'jess', school: 'B', age: 12}, " +
            "{name: 'jeff', school: 'A', age: 12}" +
            "]");

        assertThat(projectDocument(document, json("students: {$elemMatch: {school: 'B'}}"), "_id"))
            .isEqualTo(json("_id: 1, students: [{name: 'jess', school: 'B', age: 12}]"));

        assertThat(projectDocument(document, json("students: {$elemMatch: {school: 'A', age: {$gt: 10}}}"), "_id"))
            .isEqualTo(json("_id: 1, students: [{name: 'jeff', school: 'A', age: 12}]"));

        assertThat(projectDocument(document, json("students: {$elemMatch: {school: 'C'}}"), "_id"))
            .isEqualTo(json("_id: 1"));

        assertThat(projectDocument(json("_id: 1, students: [1, 2, 3]"), json("students: {$elemMatch: {school: 'C'}}"), "_id"))
            .isEqualTo(json("_id: 1"));

        assertThat(projectDocument(json("_id: 1, students: {school: 'C'}"), json("students: {$elemMatch: {school: 'C'}}"), "_id"))
            .isEqualTo(json("_id: 1"));

        assertThat(projectDocument(document, json("students: {$elemMatch: {school: 'A'}}"), "_id"))
            .isEqualTo(json("_id: 1, students: [{name: 'john', school: 'A', age: 10}]"));
    }


    @Test
    void testProjectWithSlice() throws Exception {
        Document document = json("_id: 1, values: [1, 2, 3, 4], value: 'other'");

        assertThat(projectDocument(document, json("values: {$slice: 2}"), "_id"))
            .isEqualTo(json("_id: 1, values: [1, 2]"));

        assertThat(projectDocument(document, json("values: {$slice: 20}"), "_id"))
            .isEqualTo(json("_id: 1, values: [1, 2, 3, 4]"));

        assertThat(projectDocument(document, json("values: {$slice: -2}"), "_id"))
            .isEqualTo(json("_id: 1, values: [3, 4]"));

        assertThat(projectDocument(document, json("values: {$slice: -10}"), "_id"))
            .isEqualTo(json("_id: 1, values: [1, 2, 3, 4]"));

        assertThat(projectDocument(document, json("values: {$slice: [-2, 1]}"), "_id"))
            .isEqualTo(json("_id: 1, values: [3]"));

        assertThat(projectDocument(document, json("value: {$slice: 2}"), "_id"))
            .isEqualTo(json("_id: 1, value: 'other'"));
    }

    private Document projectDocument(Document document, Document fields) {
        return projectDocument(document, fields, "_id");
    }

    private Document projectDocument(Document document, Document fields, String idField) {
        return new Projection(fields, idField).projectDocument(document);
    }

}
