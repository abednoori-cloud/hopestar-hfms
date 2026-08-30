package com.hopestar.hfms.module.finance.loan.controller;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.finance.employee.service.EmployeeService;
import com.hopestar.hfms.module.finance.ledger.service.PaymentMethodService;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanCreateDTO;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanResponseDTO;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanSearchDTO;
import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanUpdateDTO;
import com.hopestar.hfms.module.finance.loan.dto.LoanRepaymentCreateDTO;
import com.hopestar.hfms.module.finance.loan.dto.LoanRepaymentResponseDTO;
import com.hopestar.hfms.module.finance.loan.entity.LoanStatus;
import com.hopestar.hfms.module.finance.loan.service.EmployeeLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC controller for the Employee Loans module (Phase 3E): list/search/
 * sort/paginate, view, create draft, edit draft, disburse, void, record
 * repayments. Thin per the module architecture -- mirrors {@code
 * SalaryController}'s draft/action shape; all business logic lives in
 * {@link EmployeeLoanService}.
 */
@Controller
@RequestMapping("/loans")
@RequiredArgsConstructor
public class EmployeeLoanController {

    private final EmployeeLoanService employeeLoanService;
    private final EmployeeService employeeService;
    private final PaymentMethodService paymentMethodService;

    // ---------------------------------------------------------------
    // List / Search / Sort / Paginate
    // ---------------------------------------------------------------

    @GetMapping
    public String list(@ModelAttribute("searchDTO") EmployeeLoanSearchDTO searchDTO, Model model) {
        PageResponse<EmployeeLoanResponseDTO> loansPage = employeeLoanService.search(searchDTO);
        model.addAttribute("loansPage", loansPage);
        model.addAttribute("searchDTO", searchDTO);
        return "loans/list";
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long employeeId, Model model) {
        EmployeeLoanCreateDTO createDTO = new EmployeeLoanCreateDTO();
        createDTO.setEmployeeId(employeeId);
        model.addAttribute("createDTO", createDTO);
        populateFormLookups(model, employeeId);
        model.addAttribute("isEdit", false);
        return "loans/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("createDTO") EmployeeLoanCreateDTO createDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormLookups(model, createDTO.getEmployeeId());
            model.addAttribute("isEdit", false);
            return "loans/form";
        }
        try {
            EmployeeLoanResponseDTO created = employeeLoanService.create(createDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Loan " + created.getLoanNumber() + " was created as a draft.");
            return "redirect:/loans/" + created.getId();
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormLookups(model, createDTO.getEmployeeId());
            model.addAttribute("isEdit", false);
            return "loans/form";
        }
    }

    // ---------------------------------------------------------------
    // View
    // ---------------------------------------------------------------

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("loan", employeeLoanService.getById(id));
        model.addAttribute("paymentMethods", paymentMethodService.listActive());
        return "loans/view";
    }

    // ---------------------------------------------------------------
    // Edit Draft
    // ---------------------------------------------------------------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        EmployeeLoanResponseDTO existing = employeeLoanService.getById(id);
        if (existing.getStatus() != LoanStatus.DRAFT) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Loan " + existing.getLoanNumber() + " can no longer be edited because it is "
                            + existing.getStatus() + ", not DRAFT.");
            return "redirect:/loans/" + id;
        }

        EmployeeLoanUpdateDTO updateDTO = new EmployeeLoanUpdateDTO();
        updateDTO.setLoanAmount(existing.getLoanAmount());
        updateDTO.setCurrency(existing.getCurrency());
        updateDTO.setExchangeRateToUsd(existing.getExchangeRateToUsd());
        updateDTO.setMonthlyDeduction(existing.getMonthlyDeduction());
        updateDTO.setLoanDate(existing.getLoanDate());
        updateDTO.setStartMonth(existing.getStartMonth());
        updateDTO.setStartYear(existing.getStartYear());
        updateDTO.setEndMonth(existing.getEndMonth());
        updateDTO.setEndYear(existing.getEndYear());
        updateDTO.setRemarks(existing.getRemarks());

        model.addAttribute("loanId", id);
        model.addAttribute("loan", existing);
        model.addAttribute("updateDTO", updateDTO);
        populateFormLookups(model, existing.getEmployeeId());
        model.addAttribute("isEdit", true);
        return "loans/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("updateDTO") EmployeeLoanUpdateDTO updateDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("loanId", id);
            model.addAttribute("loan", employeeLoanService.getById(id));
            populateFormLookups(model, null);
            model.addAttribute("isEdit", true);
            return "loans/form";
        }
        try {
            EmployeeLoanResponseDTO updated = employeeLoanService.update(id, updateDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Loan " + updated.getLoanNumber() + " was updated.");
            return "redirect:/loans/" + id;
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("loanId", id);
            model.addAttribute("loan", employeeLoanService.getById(id));
            populateFormLookups(model, null);
            model.addAttribute("isEdit", true);
            return "loans/form";
        }
    }

    // ---------------------------------------------------------------
    // Disburse / Void
    // ---------------------------------------------------------------

    @PostMapping("/{id}/disburse")
    public String disburse(@PathVariable Long id,
                            @RequestParam Long paymentMethodId,
                            RedirectAttributes redirectAttributes) {
        try {
            EmployeeLoanResponseDTO disbursed = employeeLoanService.disburse(id, paymentMethodId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Loan " + disbursed.getLoanNumber() + " was disbursed.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/loans/" + id;
    }

    @PostMapping("/{id}/void")
    public String voidLoan(@PathVariable Long id,
                            @RequestParam(required = false) String reason,
                            RedirectAttributes redirectAttributes) {
        try {
            EmployeeLoanResponseDTO voided = employeeLoanService.voidLoan(id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Loan " + voided.getLoanNumber() + " was voided.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/loans/" + id;
    }

    // ---------------------------------------------------------------
    // Repayments
    // ---------------------------------------------------------------

    @GetMapping("/{id}/repayments")
    public String repaymentSchedule(@PathVariable Long id, Model model) {
        model.addAttribute("loan", employeeLoanService.getById(id));
        model.addAttribute("repayments", employeeLoanService.listRepayments(id));
        return "loans/schedule";
    }

    @PostMapping("/{id}/repayments")
    public String recordRepayment(@PathVariable Long id,
                                   @Valid @ModelAttribute("repaymentCreateDTO") LoanRepaymentCreateDTO createDTO,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please correct the repayment details and try again.");
            return "redirect:/loans/" + id + "/repayments";
        }
        try {
            LoanRepaymentResponseDTO repayment = employeeLoanService.recordRepayment(id, createDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Repayment of " + repayment.getCurrency() + " " + repayment.getAmount()
                            + " was recorded against loan " + repayment.getLoanNumber() + ".");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/loans/" + id + "/repayments";
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void populateFormLookups(Model model, Long employeeId) {
        model.addAttribute("paymentMethods", paymentMethodService.listActive());
        if (employeeId != null) {
            model.addAttribute("selectedEmployee", employeeService.getById(employeeId));
        } else {
            model.addAttribute("employees", employeeService.listActive());
        }
    }
}
