package telran.students.service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.dto.reflections.IdPhone;
import telran.students.exceptions.StudentIllegalStateException;
import telran.students.exceptions.StudentNotFoundException;
import telran.students.model.StudentDoc;
import telran.students.repo.StudentRepo;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentsServiceImpl implements StudentsService {
	final StudentRepo studentRepo;

	@Override
	@Transactional
	public Student addStudent(Student student) {
		long id = student.id();

		if (studentRepo.existsById(id)) {
			log.error("student with id {} already exists", id);
			throw new StudentIllegalStateException();
		}

		StudentDoc studentDoc = new StudentDoc(student);
		studentRepo.save(studentDoc);
		log.debug("student {} has been saved", student);
		return student;
	}

	@Override
	public Mark addMark(long id, Mark mark) {
		StudentDoc studentDoc = studentRepo.findById(id).orElseThrow(() -> new StudentNotFoundException());
		log.debug("student with id {}, old marks list {}", id, studentDoc.getMarks());
		studentDoc.getMarks().add(mark);
		studentRepo.save(studentDoc);
		log.debug("student with id {}, new marks list {}", id, studentDoc.getMarks());
		return mark;
	}

	@Override
	@Transactional
	public Student updatePhoneNumber(long id, String phoneNumber) {
		StudentDoc studentDoc = studentRepo.findById(id).orElseThrow(() -> new StudentNotFoundException());
		log.debug("student with id {}, old phone number {}, new phone number {}", id, studentDoc.getPhone(),
				phoneNumber);
		studentDoc.setPhone(phoneNumber);
		Student res = studentRepo.save(studentDoc).build();
		log.debug("Student {} has been saved ", res);
		return res;
	}

	@Override
	public Student removeStudent(long id) {
		Student res = getStudent(id);
		studentRepo.deleteById(id);
		log.debug("Student {} has been deleted ", res);
		return res;
	}

	@Override
	public Student getStudent(long id) {
		StudentDoc studentDoc = studentRepo.findStudentNoMarks(id);

		if (studentDoc == null) {
			throw new StudentNotFoundException();
		}

		log.debug("marks of found student {}", studentDoc.getMarks());
		Student student = studentDoc.build();
		log.debug("found student {}", student);
		return student;
	}

	@Override
	public List<Mark> getMarks(long id) {
		StudentDoc studentDoc = studentRepo.findStudentOnlyMarks(id);

		if (studentDoc == null) {
			throw new StudentNotFoundException();
		}

		List<Mark> res = studentDoc.getMarks();
		log.debug("phone: {}, id: {}", studentDoc.getPhone(), studentDoc.getId());
		log.debug("marks of found student {}", res);

		return res;
	}

	@Override
	public List<Student> getStudentsAllGoodMarks(int markThreshold) {
		// Will be implemented on the CW #72
		return null;
	}

	@Override
	public List<Student> getStudentsFewMarks(int nMarks) {
		// Will be implemented on the CW #72
		return null;
	}

	@Override
	public Student getStudentByPhoneNumber(String phoneNumber) {
		IdPhone idPhone = studentRepo.findByPhone(phoneNumber);
		Student res = null;

		if (idPhone != null) {
			res = new Student(idPhone.getId(), idPhone.getPhone());
		}

		log.debug("student {}", res);
		return res;
	}

	@Override
	public List<Student> getStudentsByPhonePrefix(String prefix) {
		List<IdPhone> idPhones = studentRepo.findByPhoneRegex(prefix + ".+");
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("getStudentsByPhonePrefix -> students {}", res);
		return res;
	}

	@Override
	public List<Student> getStudentsMarksDate(LocalDate date) {
		List<IdPhone> idPhones = studentRepo.findStudentByMarkDate(date);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("getStudentsMarksDate -> students {}", res);
		return res;
	}

	@Override
	public List<Student> getStudentsMarksMonthYear(int month, int year) {
		LocalDate start = LocalDate.of(year, month, 1);
		LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());
		List<IdPhone> idPhones = studentRepo.findStudentsBetweenDates(start, end);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("getStudentsMarksMonthYear -> students {}", res);
		return res;
	}

	@Override
	public List<Student> getStudentsGoodSubjectMark(String subject, int markThreshold) {
		List<IdPhone> idPhones = studentRepo.findStudentsGoodSubjectMark(subject, markThreshold);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("getStudentsGoodSubjectMark -> students {}", res);
		return res;
	}

	private List<Student> idPhonesToStudents(List<IdPhone> idPhones) {
		return idPhones.stream().map(ip -> new Student(ip.getId(), ip.getPhone())).toList();
	}
}
