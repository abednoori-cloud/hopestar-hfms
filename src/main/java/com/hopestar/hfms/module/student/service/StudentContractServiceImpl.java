package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.common.util.MoneyUtil;
import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import com.hopestar.hfms.module.finance.studentpayment.repository.StudentPaymentRepository;
import com.hopestar.hfms.module.student.dto.ProgramResponseDTO;
import com.hopestar.hfms.module.student.dto.StudentContractCreateDTO;
import com.hopestar.hfms.module.student.dto.StudentContractResponseDTO;
import com.hopestar.hfms.module.student.entity.ContractStatus;
import com.hopestar.hfms.module.student.entity.Program;
import com.hopestar.hfms.module.student.entity.Student;
import com.hopestar.hfms.module.student.entity.StudentContract;
import com.hopestar.hfms.module.student.repository.ProgramRepository;
import com.hopestar.hfms.module.student.repository.StudentContractRepository;
import com.hopestar.hfms.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentContractServiceImpl implements StudentContractService {

    private final StudentContractRepository studentContractRepository;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;

    /**
     * Introduced in Phase 3B (Student Payments) so {@link
     * #getRemainingBalanceUsd} can subtract posted payments. This is the
     * only place in the Student module that reaches into the Finance
     * module's data, and it does so read-only, through the repository
     * only — never constructing or posting a payment itself.
     */
    private final StudentPaymentRepository studentPaymentRepository;

    @Override
    @Transactional
    public StudentContractResponseDTO create(Long studentId, StudentContractCreateDTO createDTO) {
        Student student = studentRepository.findByIdAndActiveTrue(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        Program program = programRepository.findById(createDTO.getProgramId())
                .filter(Program::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Program", createDTO.getProgramId()));

        BigDecimal totalAmount = MoneyUtil.normalize(createDTO.getTotalContractAmount());
        BigDecimal discountAmount = MoneyUtil.normalize(createDTO.getDiscountAmount());
        BigDecimal registrationFee = MoneyUtil.normalize(createDTO.getRegistrationFee());

        if (MoneyUtil.isNegative(discountAmount)) {
            throw new BusinessValidationException("Discount amount cannot be negative.");
        }
        if (discountAmount.compareTo(totalAmount) > 0) {
            throw new BusinessValidationException("Discount amount cannot exceed the total contract amount.");
        }

        BigDecimal finalAmount = MoneyUtil.subtract(totalAmount, discountAmount);

        BigDecimal exchangeRate = MoneyUtil.resolveExchangeRateToUsd(createDTO.getCurrencyCode(), createDTO.getExchangeRateToUsd());
        BigDecimal usdEquivalentAmount = MoneyUtil.multiply(finalAmount, exchangeRate);

        StudentContract contract = StudentContract.builder()
                .student(student)
                .program(program)
                .totalContractAmount(totalAmount)
                .registrationFee(registrationFee)
                .discountAmount(discountAmount)
                .discountReason(createDTO.getDiscountReason())
                .finalAmount(finalAmount)
                .currencyCode(createDTO.getCurrencyCode())
                .exchangeRateToUsd(exchangeRate)
                .usdEquivalentAmount(usdEquivalentAmount)
                .contractDate(createDTO.getContractDate())
                .status(ContractStatus.ACTIVE)
                .notes(createDTO.getNotes())
                .build();

        StudentContract saved = studentContractRepository.save(contract);
        return toResponseDTO(saved);
    }

    @Override
    public StudentContractResponseDTO getById(Long contractId) {
        return toResponseDTO(getActiveContractEntity(contractId));
    }

    @Override
    public List<StudentContractResponseDTO> listByStudent(Long studentId) {
        return studentContractRepository.findByStudentIdAndActiveTrueOrderByContractDateDesc(studentId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public StudentContractResponseDTO cancel(Long contractId) {
        StudentContract contract = getActiveContractEntity(contractId);
        if (contract.getStatus() == ContractStatus.COMPLETED) {
            throw new BusinessValidationException("A completed contract cannot be cancelled.");
        }
        contract.setStatus(ContractStatus.CANCELLED);
        return toResponseDTO(contract);
    }

    @Override
    @Transactional
    public StudentContractResponseDTO complete(Long contractId) {
        StudentContract contract = getActiveContractEntity(contractId);
        if (contract.getStatus() == ContractStatus.CANCELLED) {
            throw new BusinessValidationException("A cancelled contract cannot be marked completed.");
        }
        contract.setStatus(ContractStatus.COMPLETED);
        return toResponseDTO(contract);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private StudentContract getActiveContractEntity(Long contractId) {
        return studentContractRepository.findByIdAndActiveTrue(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Student contract", contractId));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implements the approved business rule exactly:
     * {@code remaining balance = contract's usdEquivalentAmount - sum(POSTED payments' usdEquivalentAmount)}.
     * Only {@code POSTED} payments are summed — {@code DRAFT}, {@code
     * CANCELLED}, and {@code REFUNDED} payments must not affect balance,
     * per the approved business rules, which is exactly what {@code
     * StudentPaymentRepository.sumUsdEquivalentAmountByContractIdAndStatus}
     * filters for.
     */
    @Override
    public BigDecimal getRemainingBalanceUsd(Long contractId) {
        StudentContract contract = getActiveContractEntity(contractId);
        BigDecimal totalPosted = studentPaymentRepository
                .sumUsdEquivalentAmountByContractIdAndStatus(contractId, PaymentStatus.POSTED);
        return MoneyUtil.subtract(contract.getUsdEquivalentAmount(), totalPosted);
    }

    private StudentContractResponseDTO toResponseDTO(StudentContract contract) {
        Student student = contract.getStudent();
        Program program = contract.getProgram();

        ProgramResponseDTO programDTO = ProgramResponseDTO.builder()
                .id(program.getId())
                .name(program.getName())
                .destinationCountry(program.getDestinationCountry())
                .basePrice(program.getBasePrice())
                .currencyCode(program.getCurrencyCode())
                .description(program.getDescription())
                .active(program.isActive())
                .build();

        return StudentContractResponseDTO.builder()
                .id(contract.getId())
                .active(contract.isActive())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .createdBy(contract.getCreatedBy())
                .updatedBy(contract.getUpdatedBy())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .studentFullName(student.getFullName())
                .program(programDTO)
                .totalContractAmount(contract.getTotalContractAmount())
                .registrationFee(contract.getRegistrationFee())
                .discountAmount(contract.getDiscountAmount())
                .discountReason(contract.getDiscountReason())
                .finalAmount(contract.getFinalAmount())
                .remainingBalance(getRemainingBalanceUsd(contract.getId()))
                .currencyCode(contract.getCurrencyCode())
                .exchangeRateToUsd(contract.getExchangeRateToUsd())
                .usdEquivalentAmount(contract.getUsdEquivalentAmount())
                .contractDate(contract.getContractDate())
                .status(contract.getStatus())
                .notes(contract.getNotes())
                .build();
    }
}
