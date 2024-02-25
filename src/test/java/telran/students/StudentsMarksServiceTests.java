package telran.students;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.exceptions.StudentIllegalStateException;
import telran.students.exceptions.StudentNotFoundException;
import telran.students.model.StudentDoc;
import telran.students.repo.StudentRepo;
import telran.students.service.StudentsServiceImpl;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudentsMarksServiceTests {

	private static final long ID_NOT_EXIST = 2;

	private static final long ID_1 = 1;

	@Autowired
	StudentRepo studentRepo;

	@Autowired
	StudentsServiceImpl studentsService;

	Student stud1 = new Student(ID_1, "001");

	Student updStud1 = new Student(1, "new phone");
	Mark mark1 = new Mark("Math", 10, LocalDate.now());

	@Test
	@Order(1)
	void addStudent_normalState_success() {
		assertEquals(stud1, studentsService.addStudent(stud1));
		StudentDoc studentDoc = studentRepo.findById(stud1.id()).orElseThrow(() -> new StudentNotFoundException());
		assertEquals(stud1, studentDoc.build());

	}

	@Test
	@Order(2)
	void addStudent_alreadyExists_exception() {
		assertThrowsExactly(StudentIllegalStateException.class, () -> studentsService.addStudent(stud1));
	}

	@Test
	@Order(3)
	void updatePhoneNumber_normalState_success() {
		assertEquals(updStud1, studentsService.updatePhoneNumber(ID_1, "new phone"));
		assertEquals(updStud1, studentRepo.findById(ID_1).orElseThrow().build());
	}

	@Test
	@Order(3)
	void updatePhoneNumber_notFound_exception() {
		assertThrowsExactly(StudentNotFoundException.class,
				() -> studentsService.updatePhoneNumber(ID_NOT_EXIST, "002"));
	}

	@Test
	@Order(4)
	void addMark_normalState_success() {
		assertFalse(studentRepo.findById(ID_1).orElseThrow().getMarks().contains(mark1));
		assertEquals(mark1, studentsService.addMark(ID_1, mark1));
		assertTrue(studentRepo.findById(ID_1).orElseThrow().getMarks().contains(mark1));
	}

	@Test
	@Order(4)
	void addMark_notFound_exception() {
		assertThrowsExactly(StudentNotFoundException.class, () -> studentsService.addMark(ID_NOT_EXIST, mark1));
	}

}