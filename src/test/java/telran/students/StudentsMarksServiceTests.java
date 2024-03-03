package telran.students;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static telran.students.TestDb.*;
import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.dto.StudentAvgScore;
import telran.students.exceptions.StudentIllegalStateException;
import telran.students.exceptions.StudentNotFoundException;
import telran.students.model.StudentDoc;
import telran.students.repo.StudentRepo;
import telran.students.service.StudentsServiceImpl;

@SpringBootTest

class StudentsMarksServiceTests {

	@Autowired
	StudentRepo studentRepo;

	@Autowired
	StudentsServiceImpl studentsService;

	@Autowired
	TestDb testDb;

	Student stud1 = new Student(ID_NEW, "051-4567568");

	Mark mark1 = new Mark("Math", 10, LocalDate.now());

	@BeforeEach
	void setUp() {
		testDb.createDb();
	}

	@Test
	void getWorstStudents_normalFlow_success() {
		List<Long> expected1 = List.of(7l, 5l, 4l);
		assertIterableEquals(expected1, studentsService.getWorstStudents(3));
	}

	@Test
	void getBestStudents_normalFlow_success() {
		List<Long> expected = List.of(6l, 2l);
		assertIterableEquals(expected, studentsService.getBestStudents(2));

		List<Long> expected1 = List.of(6l);
		assertIterableEquals(expected1, studentsService.getBestStudents(1));
	}

	@Test
	void getStudentMarksAtDates_normalFlow_success() {
		List<Mark> expected = List.of(new Mark(SUBJECT1, 70, DATE1), new Mark(SUBJECT1, 80, DATE2));
		assertIterableEquals(expected, studentsService.getStudentMarksAtDates(ID1, DATE1, DATE2));

		assertTrue(studentsService.getStudentMarksAtDates(ID5, DATE4, DATE_NOT_EXIST).isEmpty());
	}

	@Test
	void getStudentMarksAtDates_notFound_exception() {
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.removeStudent(ID_NOT_EXIST));
	}

	@Test
	void getStudentsMarksAmountBetween_normalFlow_success() {
		List<Student> expected = List.of(students[2], students[5]);
		assertIterableEquals(expected, studentsService.getStudentsMarksAmountBetween(4, 5));

		List<Student> expected1 = List.of(students[4]);
		assertIterableEquals(expected1, studentsService.getStudentsMarksAmountBetween(1, 1));
		assertTrue(studentsService.getStudentsMarksAmountBetween(5, 6).isEmpty());
	}

	@Test
	void getStudentsAllGoodMarksSubject_normalFlow_success() {
		List<Student> expected = List.of(students[4], students[5]);
		assertIterableEquals(expected, studentsService.getStudentsAllGoodMarksSubject(SUBJECT4, 90));
		assertTrue(studentsService.getStudentsAllGoodMarksSubject(SUBJECT4, 100).isEmpty());
	}

	@Test
	void getStudentsAllGoodMarksTest() {
		List<Student> expected = List.of(students[4], students[5]);
		assertIterableEquals(expected, studentsService.getStudentsAllGoodMarks(70));
		assertTrue(studentsService.getStudentsAllGoodMarks(100).isEmpty());
	}

	@Test
	void getStudentMarksSubjectTest() {
		List<Mark> expected = List.of(new Mark(SUBJECT1, 70, DATE1), new Mark(SUBJECT1, 80, DATE2));
		assertIterableEquals(expected, studentsService.getStudentMarksSubject(ID1, SUBJECT1));
		assertTrue(studentsService.getStudentMarksSubject(ID1, SUBJECT3).isEmpty());
		assertThrowsExactly(StudentNotFoundException.class,
				() -> studentsService.getStudentMarksSubject(ID1 + 1000, SUBJECT3));
	}

	@Test
	void getStudentsFewMarks() {
		List<Student> expected = List.of(students[6]);
		assertIterableEquals(expected, studentsService.getStudentsFewMarks(1));
	}

	@Test
	void getStudentsAvgScoreGreaterTest() {
		List<StudentAvgScore> expected = List.of(new StudentAvgScore(ID6, 100), new StudentAvgScore(ID5, 95));
		assertIterableEquals(expected, studentsService.getStudentsAvgScoreGreater(90));

	}

	@Test
	void getStudentsMarksMonthYear_normalState_success() {
		List<Student> expected = List.of(students[0], students[1], students[2], students[5]);
		List<Student> res = studentsService.getStudentsMarksMonthYear(1, 2024);
		assertIterableEquals(expected, res);
		assertTrue(studentsService.getStudentsMarksMonthYear(2, 2020).isEmpty());
	}

	@Test
	void getStudentsGoodSubjectMark_normalState_success() {
		List<Student> expected = List.of(students[0], students[5]);
		List<Student> res = studentsService.getStudentsGoodSubjectMark(SUBJECT1, 80);
		assertIterableEquals(expected, res);
		assertTrue(studentsService.getStudentsGoodSubjectMark(SUBJECT_NOT_EXIST, 85).isEmpty());
	}

	@Test
	void getStudentsMarksDate_normalState_success() {
		List<Student> expected = List.of(students[0], students[1], students[2], students[5]);
		List<Student> res = studentsService.getStudentsMarksDate(DATE1);
		assertIterableEquals(expected, res);
		assertTrue(studentsService.getStudentsMarksDate(DATE_NOT_EXIST).isEmpty());
	}

	@Test
	void removeStudent_normalState_success() {
		assertEquals(students[0], studentsService.removeStudent(ID1));
		assertThrowsExactly(StudentNotFoundException.class,
				() -> studentRepo.findById(ID1).orElseThrow(() -> new StudentNotFoundException()));

	}

	@Test
	void removeStudent_notFound_exception() {
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.removeStudent(ID_NOT_EXIST));
	}

	@Test
	void addStudent_normalState_success() {
		assertEquals(stud1, studentsService.addStudent(stud1));
		StudentDoc studentDoc = studentRepo.findById(stud1.id()).orElseThrow(() -> new StudentNotFoundException());
		assertEquals(stud1, studentDoc.build());

	}

	@Test
	void addStudent_alreadyExists_exception() {
		assertThrowsExactly(StudentIllegalStateException.class, () -> studentsService.addStudent(students[0]));
	}

	@Test
	void updatePhoneNumber_normalState_success() {
		Student updStud1 = new Student(ID1, "new phone");
		assertEquals(updStud1, studentsService.updatePhoneNumber(ID1, "new phone"));
		assertEquals(updStud1, studentRepo.findById(ID1).orElseThrow().build());
	}

	@Test
	void updatePhoneNumber_notFound_exception() {
		assertThrowsExactly(StudentNotFoundException.class,
				() -> studentsService.updatePhoneNumber(ID_NOT_EXIST, "002"));
	}

	@Test
	void addMark_normalState_success() {
		assertFalse(studentRepo.findById(ID1).orElseThrow().getMarks().contains(mark1));
		assertEquals(mark1, studentsService.addMark(ID1, mark1));
		assertTrue(studentRepo.findById(ID1).orElseThrow().getMarks().contains(mark1));
	}

	@Test
	void addMark_notFound_exception() {
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.addMark(ID_NOT_EXIST, mark1));
	}

	/*************/
	@Test
	void getStudentTest() {
		assertEquals(students[0], studentsService.getStudent(ID1));
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.getStudent(100000));
	}

	@Test
	void getMarksTest() {
		assertArrayEquals(marks[0], studentsService.getMarks(ID1).toArray(Mark[]::new));
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.getMarks(100000));
	}

	@Test
	void getStudentByPhoneNumberTest() {
		assertEquals(students[0], studentsService.getStudentByPhoneNumber(PHONE1));
	}

	@Test
	void getStudentsByPhonePrefix() {
		List<Student> expected = List.of(students[0], students[6]);
		assertIterableEquals(expected, studentsService.getStudentsByPhonePrefix("051"));
	}
}