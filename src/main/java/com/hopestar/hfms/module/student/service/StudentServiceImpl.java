package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.DuplicateResourceException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.service.SequenceGeneratorService;
import com.hopestar.hfms.module.auth.entity.Branch;
import com.hopestar.hfms.module.auth.repository.BranchRepository;
import com.hopestar.hfms.module.student.dto.ProgramResponseDTO;
import com.hopestar.hfms.module.student.dto.StudentCreateDTO;
import com.hopestar.hfms.module.student.dto.StudentResponseDTO;
import com.hopestar.hfms.module.student.dto.StudentSearchDTO;
import com.hopestar.hfms.module.student.dto.StudentStatusResponseDTO;
import com.hopestar.hfms.module.student.dto.StudentUpdateDTO;
import com.hopestar.hfms.module.student.entity.Program;
import com.hopestar.hfms.module.student.entity.Student;
import com.hopestar.hfms.module.student.entity.StudentStatus;
import com.hopestar.hfms.module.student.repository.ProgramRepository;
import com.hopestar.hfms.module.student.repository.StudentRepository;
import com.hopestar.hfms.module.student.repository.StudentSpecifications;
import com.hopestar.hfms.module.student.repository.StudentStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implements {@link StudentService}. See the interface Javadoc for the
 * governing business rules (system-generated codes, conditional passport/
 * email uniqueness, soft-delete-only).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private static final String STUDENT_SEQUENCE_KEY = "STUDENT";
    private static final String STUDENT_CODE_PREFIX = "STU";
    private static final int STUDENT_CODE_PADDING = 6;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final StudentStatusRepository studentStatusRepository;
    private final BranchRepository branchRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('STUDENT_MANAGE')")
    public StudentResponseDTO create(StudentCreateDTO createDTO) {
        validateUniquePassport(createDTO.getPassportNumber(), null);
        validateUniqueEmail(createDTO.getEmail(), null);

        Program program = resolveOrCreateProgram(createDTO.getProgramName(), createDTO.getDestinationCountry());
        StudentStatus status = resolveStatus(createDTO.getStatusId());
        Branch branch = resolveBranch(createDTO.getBranchId());

        String studentCode = sequenceGeneratorService.nextYearlyValue(
                STUDENT_SEQUENCE_KEY, STUDENT_CODE_PREFIX, STUDENT_CODE_PADDING, createDTO.getBranchId());

        Student student = Student.builder()
                .studentCode(studentCode)
                .fullName(createDTO.getFullName())
                .fatherName(createDTO.getFatherName())
                .phone(createDTO.getPhone())
                .email(normalizeBlankToNull(createDTO.getEmail()))
                .passportNumber(normalizeBlankToNull(createDTO.getPassportNumber()))
                .passportExpiry(createDTO.getPassportExpiry())
                .program(program)
                .destinationCountry(program.getDestinationCountry())
                .status(status)
                .registrationDate(createDTO.getRegistrationDate())
                .notes(createDTO.getNotes())
                .branch(branch)
                .build();

        Student saved = studentRepository.save(student);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('STUDENT_MANAGE')")
    public StudentResponseDTO update(Long id, StudentUpdateDTO updateDTO) {
        Student student = getActiveStudentEntity(id);

        validateUniquePassport(updateDTO.getPassportNumber(), id);
        validateUniqueEmail(updateDTO.getEmail(), id);

        Program program = resolveOrCreateProgram(updateDTO.getProgramName(), updateDTO.getDestinationCountry());
        StudentStatus status = resolveStatus(updateDTO.getStatusId());
        Branch branch = resolveBranch(updateDTO.getBranchId());

        student.setFullName(updateDTO.getFullName());
        student.setFatherName(updateDTO.getFatherName());
        student.setPhone(updateDTO.getPhone());
        student.setEmail(normalizeBlankToNull(updateDTO.getEmail()));
        student.setPassportNumber(normalizeBlankToNull(updateDTO.getPassportNumber()));
        student.setPassportExpiry(updateDTO.getPassportExpiry());
        student.setProgram(program);
        student.setDestinationCountry(program.getDestinationCountry());
        student.setStatus(status);
        student.setRegistrationDate(updateDTO.getRegistrationDate());
        student.setNotes(updateDTO.getNotes());
        student.setBranch(branch);

        return toResponseDTO(student);
    }

    @Override
    public StudentResponseDTO getById(Long id) {
        return toResponseDTO(getActiveStudentEntity(id));
    }

    @Override
    public StudentResponseDTO getByStudentCode(String studentCode) {
        Student student = studentRepository.findByStudentCodeAndActiveTrue(studentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentCode));
        return toResponseDTO(student);
    }

    @Override
    public List<StudentResponseDTO> listActive() {
        return studentRepository.findByActiveTrueOrderByFullNameAsc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public PageResponse<StudentResponseDTO> search(StudentSearchDTO searchDTO) {
        int page = Math.max(searchDTO.getPage(), 0);
        int size = searchDTO.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(searchDTO.getSize(), MAX_PAGE_SIZE);

        Page<Student> result = studentRepository.findAll(
                StudentSpecifications.fromSearchCriteria(searchDTO),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.from(result.map(this::toResponseDTO));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('STUDENT_MANAGE')")
    public void deactivate(Long id) {
        Student student = getActiveStudentEntity(id);
        student.softDelete();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('STUDENT_MANAGE')")
    public void reactivate(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        student.restore();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Student getActiveStudentEntity(Long id) {
        return studentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    /**
     * Looks up an active {@link Program} by name + destination country
     * (case-insensitive, matching the {@code uk_programs_name_country}
     * constraint) and links to it; if none matches, auto-creates one so
     * staff can enter a program freely on the student form without first
     * curating the programs master list. Auto-created rows get a
     * placeholder {@code basePrice} of {@code 0.00} in the base currency
     * (USD) — there is no Program admin screen yet to edit that price, so
     * a real price must be set for the row before it is used to price a
     * {@link com.hopestar.hfms.module.student.entity.StudentContract}.
     */
    private Program resolveOrCreateProgram(String programName, String destinationCountry) {
        String trimmedName = programName.trim();
        String trimmedCountry = destinationCountry.trim();
        return programRepository.findByNameIgnoreCaseAndDestinationCountryIgnoreCase(trimmedName, trimmedCountry)
                .orElseGet(() -> programRepository.save(Program.builder()
                        .name(trimmedName)
                        .destinationCountry(trimmedCountry)
                        .basePrice(BigDecimal.ZERO)
                        .currencyCode(SupportedCurrency.USD)
                        .build()));
    }

    private StudentStatus resolveStatus(Long statusId) {
        return studentStatusRepository.findById(statusId)
                .filter(StudentStatus::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Student status", statusId));
    }

    private Branch resolveBranch(Long branchId) {
        if (branchId == null) {
            return branchRepository.findFirstByHeadquartersTrueAndActiveTrue().orElse(null);
        }
        return branchRepository.findById(branchId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
    }

    private void validateUniquePassport(String passportNumber, Long excludingId) {
        if (!StringUtils.hasText(passportNumber)) {
            return;
        }
        boolean duplicate = excludingId == null
                ? studentRepository.existsByPassportNumber(passportNumber)
                : studentRepository.existsByPassportNumberAndIdNot(passportNumber, excludingId);
        if (duplicate) {
            throw new DuplicateResourceException("Student", "passport number", passportNumber);
        }
    }

    private void validateUniqueEmail(String email, Long excludingId) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        boolean duplicate = excludingId == null
                ? studentRepository.existsByEmail(email)
                : studentRepository.existsByEmailAndIdNot(email, excludingId);
        if (duplicate) {
            throw new DuplicateResourceException("Student", "email", email);
        }
    }

    private String normalizeBlankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private StudentResponseDTO toResponseDTO(Student student) {
        if (student.getProgram() == null) {
            throw new BusinessValidationException("Student " + student.getId() + " has no associated program.");
        }
        ProgramResponseDTO programDTO = ProgramResponseDTO.builder()
                .id(student.getProgram().getId())
                .name(student.getProgram().getName())
                .destinationCountry(student.getProgram().getDestinationCountry())
                .basePrice(student.getProgram().getBasePrice())
                .currencyCode(student.getProgram().getCurrencyCode())
                .description(student.getProgram().getDescription())
                .active(student.getProgram().isActive())
                .build();

        StudentStatusResponseDTO statusDTO = StudentStatusResponseDTO.builder()
                .id(student.getStatus().getId())
                .name(student.getStatus().getName())
                .description(student.getStatus().getDescription())
                .build();

        return StudentResponseDTO.builder()
                .id(student.getId())
                .active(student.isActive())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .createdBy(student.getCreatedBy())
                .updatedBy(student.getUpdatedBy())
                .studentCode(student.getStudentCode())
                .fullName(student.getFullName())
                .fatherName(student.getFatherName())
                .phone(student.getPhone())
                .email(student.getEmail())
                .passportNumber(student.getPassportNumber())
                .passportExpiry(student.getPassportExpiry())
                .program(programDTO)
                .destinationCountry(student.getDestinationCountry())
                .status(statusDTO)
                .registrationDate(student.getRegistrationDate())
                .notes(student.getNotes())
                .branchName(student.getBranch() != null ? student.getBranch().getName() : null)
                .contractCount(student.getContracts() != null ? student.getContracts().size() : 0)
                .documentCount(student.getDocuments() != null ? student.getDocuments().size() : 0)
                .build();
    }
}
