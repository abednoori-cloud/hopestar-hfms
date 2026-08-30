package com.hopestar.hfms.module.finance.studentpayment.controller;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.finance.ledger.service.CurrencyService;
import com.hopestar.hfms.module.finance.ledger.service.PaymentMethodService;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentCreateDTO;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentResponseDTO;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentSearchDTO;
import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentUpdateDTO;
import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import com.hopestar.hfms.module.finance.studentpayment.service.StudentPaymentService;
import com.hopestar.hfms.module.student.dto.StudentContractResponseDTO;
import com.hopestar.hfms.module.student.service.StudentContractService;
import com.hopestar.hfms.module.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * MVC controller for the Student Payments module (Phase 3B): list/search,
 * view, create, edit draft, post a draft, cancel, and per-student payment
 * history. Thin per the module architecture -- all business logic and
 * validation lives in {@link StudentPaymentService}.
 */
@Controller
@RequiredArgsConstructor
public class StudentPaymentController {

    private final StudentPaymentService studentPaymentService;
    private final StudentService studentService;
    private final StudentContractService studentContractService;
    private final CurrencyService currencyService;
    private final PaymentMethodService paymentMethodService;

    // ---------------------------------------------------------------
    // List / Search
    // ---------------------------------------------------------------

    @GetMapping("/payments")
    public String list(@ModelAttribute("searchDTO") StudentPaymentSearchDTO searchDTO, Model model) {
        PageResponse<StudentPaymentResponseDTO> paymentsPage = studentPaymentService.search(searchDTO);
        model.addAttribute("paymentsPage", paymentsPage);
        model.addAttribute("searchDTO", searchDTO);
        model.addAttribute("currencies", currencyService.listActive());
        model.addAttribute("paymentMethods", paymentMethodService.listActive());
        return "payments/list";
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @GetMapping("/payments/new")
    public String newForm(@RequestParam(required = false) Long studentId,
                           @RequestParam(required = false) Long contractId,
                           Model model) {
        StudentPaymentCreateDTO createDTO = new StudentPaymentCreateDTO();
        createDTO.setStudentId(studentId);
        createDTO.setContractId(contractId);

        model.addAttribute("createDTO", createDTO);
        populateFormLookups(model, studentId);
        model.addAttribute("isEdit", false);
        return "payments/form";
    }

    @PostMapping("/payments")
    public String create(@Valid @ModelAttribute("createDTO") StudentPaymentCreateDTO createDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormLookups(model, createDTO.getStudentId());
            model.addAttribute("isEdit", false);
            return "payments/form";
        }
        try {
            StudentPaymentResponseDTO created = studentPaymentService.create(createDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment " + created.getPaymentNumber() + " (receipt " + created.getReceiptNumber()
                            + ") was recorded successfully.");
            return "redirect:/payments/" + created.getId();
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormLookups(model, createDTO.getStudentId());
            model.addAttribute("isEdit", false);
            return "payments/form";
        }
    }

    // ---------------------------------------------------------------
    // View
    // ---------------------------------------------------------------

    @GetMapping("/payments/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("payment", studentPaymentService.getById(id));
        return "payments/view";
    }

    // ---------------------------------------------------------------
    // Edit Draft
    // ---------------------------------------------------------------

    @GetMapping("/payments/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        StudentPaymentResponseDTO existing = studentPaymentService.getById(id);
        if (existing.getStatus() != PaymentStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Payment " + existing.getPaymentNumber() + " can no longer be edited because it is "
                            + existing.getStatus() + ", not DRAFT.");
        }

        StudentPaymentUpdateDTO updateDTO = new StudentPaymentUpdateDTO();
        updateDTO.setPaymentDate(existing.getPaymentDate());
        updateDTO.setOriginalAmount(existing.getOriginalAmount());
        updateDTO.setCurrencyCode(SupportedCurrency.valueOf(existing.getCurrency().getCode()));
        updateDTO.setExchangeRateToUsd(existing.getExchangeRateToUsd());
        updateDTO.setPaymentMethodId(existing.getPaymentMethod().getId());
        updateDTO.setReferenceNumber(existing.getReferenceNumber());
        updateDTO.setNotes(existing.getNotes());

        model.addAttribute("paymentId", id);
        model.addAttribute("payment", existing);
        model.addAttribute("updateDTO", updateDTO);
        populateFormLookups(model, existing.getStudentId());
        model.addAttribute("isEdit", true);
        return "payments/form";
    }

    @PostMapping("/payments/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("updateDTO") StudentPaymentUpdateDTO updateDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("paymentId", id);
            model.addAttribute("payment", studentPaymentService.getById(id));
            populateFormLookups(model, null);
            model.addAttribute("isEdit", true);
            return "payments/form";
        }
        try {
            StudentPaymentResponseDTO updated = studentPaymentService.update(id, updateDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment " + updated.getPaymentNumber() + " was updated.");
            return "redirect:/payments/" + id;
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("paymentId", id);
            model.addAttribute("payment", studentPaymentService.getById(id));
            populateFormLookups(model, null);
            model.addAttribute("isEdit", true);
            return "payments/form";
        }
    }

    // ---------------------------------------------------------------
    // Post a draft / Cancel
    // ---------------------------------------------------------------

    @PostMapping("/payments/{id}/post")
    public String post(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            StudentPaymentResponseDTO posted = studentPaymentService.post(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment " + posted.getPaymentNumber() + " was posted to the ledger.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/payments/" + id;
    }

    @PostMapping("/payments/{id}/cancel")
    public String cancel(@PathVariable Long id,
                          @RequestParam(required = false) String reason,
                          RedirectAttributes redirectAttributes) {
        try {
            StudentPaymentResponseDTO cancelled = studentPaymentService.cancel(id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment " + cancelled.getPaymentNumber() + " was cancelled.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/payments/" + id;
    }

    // ---------------------------------------------------------------
    // Payment History (per student)
    // ---------------------------------------------------------------

    @GetMapping("/students/{studentId}/payments")
    public String history(@PathVariable Long studentId, Model model) {
        model.addAttribute("student", studentService.getById(studentId));
        List<StudentPaymentResponseDTO> payments = studentPaymentService.listByStudent(studentId);
        model.addAttribute("payments", payments);
        return "payments/history";
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void populateFormLookups(Model model, Long studentId) {
        model.addAttribute("currencies", currencyService.listActive());
        model.addAttribute("paymentMethods", paymentMethodService.listActive());
        if (studentId != null) {
            model.addAttribute("selectedStudent", studentService.getById(studentId));
            List<StudentContractResponseDTO> contracts = studentContractService.listByStudent(studentId);
            model.addAttribute("studentContracts", contracts);
        } else {
            model.addAttribute("students", studentService.listActive());
        }
    }
}
