package telran.students.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import telran.students.dto.reflections.IdPhone;
import telran.students.model.StudentDoc;

public interface StudentRepo extends MongoRepository<StudentDoc, Long> {
	@Query(value = "{id:?0}", fields = "{id:1, phone:1}")
	StudentDoc findStudentNoMarks(long id);

	@Query(value = "{id:?0}", fields = "{id:0, marks:1}")
	StudentDoc findStudentOnlyMarks(long id);

	IdPhone findByPhone(String phone);

	List<IdPhone> findByPhoneRegex(String regex);

	@Query(value = "{'marks.date':?0}")
	List<StudentDoc> findStudentByMarkDate(LocalDate date);

	@Query(value = "{'marks': {$elemMatch: {subject:?0, score: {$gte:?1} }}}")
	List<StudentDoc> findStudentsGoodSubjectMark(String subject, int markThreshold);

	@Query(value = "{'marks': {$elemMatch: {date:{$gte:?0}, date:{$lt:?1} }}}")
	List<StudentDoc> findStudentsBetweenDates(LocalDate start, LocalDate end);

}