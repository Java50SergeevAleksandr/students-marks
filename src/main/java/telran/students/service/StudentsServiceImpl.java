package telran.students.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort.Direction;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;

import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.dto.StudentAvgScore;
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
	final MongoTemplate mongoTemplate;

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
		List<IdPhone> idPhones = studentRepo.findAllGoodMarks(markThreshold);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("students having marks greater than {} are {}", markThreshold, res);
		return res;
	}

	@Override
	public List<Student> getStudentsFewMarks(int nMarks) {
		List<IdPhone> idPhones = studentRepo.findFewMarks(nMarks);
		List<Student> res = idPhonesToStudents(idPhones);
		log.debug("student having amount of marks less than {} are {}", nMarks, res);
		return res;
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

	@Override
	public List<Mark> getStudentMarksSubject(long id, String subject) {
		if (!studentRepo.existsById(id)) {
			throw new StudentNotFoundException();
		}
		MatchOperation matchStudentOperation = Aggregation.match(Criteria.where("id").is(id));
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		MatchOperation matchSubject = Aggregation.match(Criteria.where("marks.subject").is(subject));
		ProjectionOperation projectOperation = Aggregation.project("marks.subject", "marks.score", "marks.date");
		Aggregation pipeline = Aggregation.newAggregation(matchStudentOperation, unwindOperation, matchSubject,
				projectOperation);
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> documents = aggregationResult.getMappedResults();
		log.debug("received {} documents", documents.size());
		List<Mark> res = documents.stream().map(d -> new Mark(d.getString("subject"), d.getInteger("score"),
				d.getDate("date").toInstant().atZone(ZoneId.systemDefault()).toLocalDate())).toList();
		log.debug("marks of subject {} of student {} are {}", subject, id, res);
		return res;
	}

	@Override
	public List<StudentAvgScore> getStudentsAvgScoreGreater(int avgThreshold) {
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		GroupOperation groupOperation = Aggregation.group("id").avg("marks.score").as("avgScore");
		MatchOperation matchOperation = Aggregation.match(Criteria.where("avgScore").gt(avgThreshold));
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "avgScore");
		Aggregation pipeline = Aggregation.newAggregation(unwindOperation, groupOperation, matchOperation,
				sortOperation);
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> documents = aggregationResult.getMappedResults();
		List<StudentAvgScore> res = documents.stream()
				.map(d -> new StudentAvgScore(d.getLong("_id"), d.getDouble("avgScore").intValue())).toList();
		log.debug("students with avg scores greater than {} are {}", avgThreshold, res);
		return res;
	}

	@Override
	public List<Student> getStudentsAllGoodMarksSubject(String subject, int thresholdScore) {
		List<IdPhone> IdPhones = studentRepo.findAllGoodMarksInSubject(subject, thresholdScore);
		List<Student> res = idPhonesToStudents(IdPhones);
		log.debug("students having marks greater than {} in subject {} are {}", thresholdScore, subject, res);
		return res;
	}

	@Override
	public List<Student> getStudentsMarksAmountBetween(int min, int max) {
		List<IdPhone> IdPhones = studentRepo.findMarksAmountBetween(min, max);
		List<Student> res = idPhonesToStudents(IdPhones);
		log.debug("students having marks between {} and {}, are {}", min, max, res);
		return res;
	}

	@Override
	public List<Mark> getStudentMarksAtDates(long id, LocalDate from, LocalDate to) {

		if (!studentRepo.existsById(id)) {
			throw new StudentNotFoundException();
		}

		MatchOperation matchStudentOperation = Aggregation.match(Criteria.where("id").is(id));
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		MatchOperation matchOperation = Aggregation.match(Criteria.where("marks.date").gte(from).lte(to));
		ProjectionOperation projectOperation = Aggregation.project("marks.subject", "marks.score", "marks.date");
		Aggregation pipeline = Aggregation.newAggregation(matchStudentOperation, unwindOperation, matchOperation,
				projectOperation);
		var aggregationResult = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> documents = aggregationResult.getMappedResults();
		log.debug("received {} documents", documents.size());
		List<Mark> res = documents.stream().map(d -> new Mark(d.getString("subject"), d.getInteger("score"),
				d.getDate("date").toInstant().atZone(ZoneId.systemDefault()).toLocalDate())).toList();
		log.debug("marks of student {} from date {} to date {} are {}", id, from, to, res);
		return res;
	}

	@Override
	public List<Long> getBestStudents(int nStudents) {
		int criteria = 80;
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		MatchOperation matchOperation = Aggregation.match(Criteria.where("marks.score").gt(criteria));
		GroupOperation groupOperation = Aggregation.group("id").count().as("nScores");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "nScores");
		LimitOperation limitOperation = Aggregation.limit(nStudents);
		ProjectionOperation projectOperation = Aggregation.project("id");
		Aggregation pipeline = Aggregation.newAggregation(unwindOperation, matchOperation, groupOperation,
				sortOperation, limitOperation, projectOperation);
		var aggregResults = mongoTemplate.aggregate(pipeline, StudentDoc.class, Document.class);
		List<Document> documents = aggregResults.getMappedResults();
		List<Long> res = documents.stream().map(d -> d.getLong("_id")).toList();

		log.debug("received {} documents", res.size());
		log.debug("{} best students  are {}", nStudents, res);
		return res;
	}

	@Override
	public List<Long> getWorstStudents(int nStudents) {
		AggregationExpression expression = AccumulatorOperators.Sum.sumOf("marks.score");
		ProjectionOperation projectionOperation = Aggregation.project("id").and(expression).as("sumScore");
		SortOperation sortOperation = Aggregation.sort(Direction.ASC, "sumScore");
		LimitOperation limitOperation = Aggregation.limit(nStudents);
		ProjectionOperation projectionOperationOnlyId = Aggregation.project("id");
		Aggregation pipeLine = Aggregation.newAggregation(projectionOperation, sortOperation, limitOperation,
				projectionOperationOnlyId);
		List<Long> res = mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class).getMappedResults().stream()
				.map(d -> d.getLong("id")).toList();
		log.debug("{} worst students are {}", nStudents, res);
		return res;
	}
}
