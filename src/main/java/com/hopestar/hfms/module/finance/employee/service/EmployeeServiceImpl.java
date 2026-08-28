package com.hopestar.hfms.module.finance.employee.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.DuplicateResourceException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.service.SequenceGeneratorService;
import com.hopestar.hfms.common.util.MoneyUtil;
import com.hopestar.hfms.module.auth.entity.Branch;
import com.hopestar.hfms.module.auth.repository.BranchRepository;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeCreateDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeResponseDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeSearchDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeStatusResponseDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeUpdateDTO;
import com.hopestar.hfms.module.finance.employee.entity.Employee;
import com.hopestar.hfms.module.finance.employee.entity.EmployeeStatus;
import com.hopestar.hfms.module.finance.employee.repository.EmployeeRepository;
import com.hopestar.hfms.module.finance.employee.repository.EmployeeSpecifications;
import com.hopestar.hfms.module.finance.employee.repository.EmployeeStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Implements {@link EmployeeService}. See that interface's Javadoc for
 * the governing business rules.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private static final String EMPLOYEE_SEQUENCE_KEY = "EMPLOYEE";
    private static final String EMPLOYEE_CODE_PREFIX = "EMP";
    private static final int EMPLOYEE_CODE_PADDING = 6;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeRepository employeeRepository;
    private final EmployeeStatusRepository employeeStatusRepository;
    private final BranchRepository branchRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Override
    @Transactional
    public EmployeeResponseDTO create(EmployeeCreateDTO createDTO) {
        validateUniquePhone(createDTO.getPhone(), null);
        validateUniqueEmail(createDTO.getEmail(), null);
        validateUniqueNationalId(createDTO.getNationalId(), null);

        EmployeeStatus status = resolveStatus(createDTO.getEmploymentStatusId());
        Branch branch = resolveBranch(createDTO.getBranchId());

        BigDecimal baseSalary = MoneyUtil.normalize(createDTO.getBaseSalary());
        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(createDTO.getSalaryCurrency(), createDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentSalary = MoneyUtil.multiply(baseSalary, exchangeRate);

        String employeeCode = sequenceGeneratorService.nextYearlyValue(
                EMPLOYEE_SEQUENCE_KEY, EMPLOYEE_CODE_PREFIX, EMPLOYEE_CODE_PADDING, createDTO.getBranchId());

        Employee employee = Employee.builder()
                .employeeCode(employeeCode)
                .fullName(createDTO.getFullName())
                .fatherName(createDTO.getFatherName())
                .gender(createDTO.getGender())
                .dateOfBirth(createDTO.getDateOfBirth())
                .phone(normalizeBlankToNull(createDTO.getPhone()))
                .email(normalizeBlankToNull(createDTO.getEmail()))
                .nationalId(normalizeBlankToNull(createDTO.getNationalId()))
                .address(createDTO.getAddress())
                .position(createDTO.getPosition())
                .department(createDTO.getDepartment())
                .joiningDate(createDTO.getJoiningDate())
                .employmentStatus(status)
                .baseSalary(baseSalary)
                .salaryCurrency(createDTO.getSalaryCurrency())
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentSalary(usdEquivalentSalary)
                .notes(createDTO.getNotes())
                .branch(branch)
                .build();

        Employee saved = employeeRepository.save(employee);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO update(Long id, EmployeeUpdateDTO updateDTO) {
        Employee employee = getActiveEmployeeEntity(id);

        validateUniquePhone(updateDTO.getPhone(), id);
        validateUniqueEmail(updateDTO.getEmail(), id);
        validateUniqueNationalId(updateDTO.getNationalId(), id);

        EmployeeStatus status = resolveStatus(updateDTO.getEmploymentStatusId());
        Branch branch = resolveBranch(updateDTO.getBranchId());

        BigDecimal baseSalary = MoneyUtil.normalize(updateDTO.getBaseSalary());
        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(updateDTO.getSalaryCurrency(), updateDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentSalary = MoneyUtil.multiply(baseSalary, exchangeRate);

        employee.setFullName(updateDTO.getFullName());
        employee.setFatherName(updateDTO.getFatherName());
        employee.setGender(updateDTO.getGender());
        employee.setDateOfBirth(updateDTO.getDateOfBirth());
        employee.setPhone(normalizeBlankToNull(updateDTO.getPhone()));
        employee.setEmail(normalizeBlankToNull(updateDTO.getEmail()));
        employee.setNationalId(normalizeBlankToNull(updateDTO.getNationalId()));
        employee.setAddress(updateDTO.getAddress());
        employee.setPosition(updateDTO.getPosition());
        employee.setDepartment(updateDTO.getDepartment());
        employee.setJoiningDate(updateDTO.getJoiningDate());
        employee.setEmploymentStatus(status);
        employee.setBaseSalary(baseSalary);
        employee.setSalaryCurrency(updateDTO.getSalaryCurrency());
        employee.setExchangeRateToUsd(exchangeRate);
        employee.setUsdEquivalentSalary(usdEquivalentSalary);
        employee.setNotes(updateDTO.getNotes());
        employee.setBranch(branch);

        return toResponseDTO(employee);
    }

    @Override
    public EmployeeResponseDTO getById(Long id) {
        return toResponseDTO(getActiveEmployeeEntity(id));
    }

    @Override
    public EmployeeResponseDTO getByEmployeeCode(String employeeCode) {
        Employee employee = employeeRepository.findByEmployeeCodeAndActiveTrue(employeeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeCode));
        return toResponseDTO(employee);
    }

    @Override
    public PageResponse<EmployeeResponseDTO> search(EmployeeSearchDTO searchDTO) {
        int page = Math.max(searchDTO.getPage(), 0);
        int size = searchDTO.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(searchDTO.getSize(), MAX_PAGE_SIZE);

        String sortField = EmployeeSpecifications.resolveSortField(searchDTO.getSortBy());
        Sort.Direction direction = "DESC".equalsIgnoreCase(searchDTO.getSortDirection())
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Page<Employee> result = employeeRepository.findAll(
                EmployeeSpecifications.fromSearchCriteria(searchDTO),
                PageRequest.of(page, size, Sort.by(direction, sortField)));

        return PageResponse.from(result.map(this::toResponseDTO));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Employee employee = getActiveEmployeeEntity(id);
        employee.softDelete();
    }

    @Override
    @Transactional
    public void reactivate(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        employee.restore();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Employee getActiveEmployeeEntity(Long id) {
        return employeeRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    private EmployeeStatus resolveStatus(Long statusId) {
        return employeeStatusRepository.findById(statusId)
                .filter(EmployeeStatus::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Employee status", statusId));
    }

    private Branch resolveBranch(Long branchId) {
        if (branchId == null) {
            return branchRepository.findFirstByHeadquartersTrueAndActiveTrue().orElse(null);
        }
        return branchRepository.findById(branchId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
    }

    private void validateUniquePhone(String phone, Long excludingId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        boolean duplicate = excludingId == null
                ? employeeRepository.existsByPhone(phone)
                : employeeRepository.existsByPhoneAndIdNot(phone, excludingId);
        if (duplicate) {
            throw new DuplicateResourceException("Employee", "phone", phone);
        }
    }

    private void validateUniqueEmail(String email, Long excludingId) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        boolean duplicate = excludingId == null
                ? employeeRepository.existsByEmail(email)
                : employeeRepository.existsByEmailAndIdNot(email, excludingId);
        if (duplicate) {
            throw new DuplicateResourceException("Employee", "email", email);
        }
    }

    private void validateUniqueNationalId(String nationalId, Long excludingId) {
        if (!StringUtils.hasText(nationalId)) {
            return;
        }
        boolean duplicate = excludingId == null
                ? employeeRepository.existsByNationalId(nationalId)
                : employeeRepository.existsByNationalIdAndIdNot(nationalId, excludingId);
        if (duplicate) {
            throw new DuplicateResourceException("Employee", "national ID", nationalId);
        }
    }

    private String normalizeBlankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private EmployeeResponseDTO toResponseDTO(Employee employee) {
        EmployeeStatusResponseDTO statusDTO = EmployeeStatusResponseDTO.builder()
                .id(employee.getEmploymentStatus().getId())
                .name(employee.getEmploymentStatus().getName())
                .description(employee.getEmploymentStatus().getDescription())
                .build();

        return EmployeeResponseDTO.builder()
                .id(employee.getId())
                .active(employee.isActive())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .createdBy(employee.getCreatedBy())
                .updatedBy(employee.getUpdatedBy())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .fatherName(employee.getFatherName())
                .gender(employee.getGender())
                .dateOfBirth(employee.getDateOfBirth())
                .phone(employee.getPhone())
                .email(employee.getEmail())
                .nationalId(employee.getNationalId())
                .address(employee.getAddress())
                .position(employee.getPosition())
                .department(employee.getDepartment())
                .joiningDate(employee.getJoiningDate())
                .employmentStatus(statusDTO)
                .baseSalary(employee.getBaseSalary())
                .salaryCurrency(employee.getSalaryCurrency())
                .exchangeRateToUsd(employee.getExchangeRateToUsd())
                .usdEquivalentSalary(employee.getUsdEquivalentSalary())
                .notes(employee.getNotes())
                .branchName(employee.getBranch() != null ? employee.getBranch().getName() : null)
                .build();
    }
}
