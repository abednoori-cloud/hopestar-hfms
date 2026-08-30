package com.hopestar.hfms.module.finance.salary.controller;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.DuplicateResourceException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.finance.employee.service.EmployeeService;
import com.hopestar.hfms.module.finance.ledger.service.PaymentMethodService;
import com.hopestar.hfms.module.finance.salary.dto.SalaryCreateDTO;
import com.hopestar.hfms.module.finance.salary.dto.SalaryResponseDTO;
import com.hopestar.hfms.module.finance.salary.dto.SalarySearchDTO;
import com.hopestar.hfms.module.finance.salary.dto.SalaryUpdateDTO;
import com.hopestar.hfms.module.finance.salary.entity.SalaryPaymentStatus;
import com.hopestar.hfms.module.finance.salary.service.SalaryService;
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
 * MVC controller for the Salary Management module (Phase 3D): list/
 * search/sort/paginate, view, create, edit draft, post, void. Thin per
 * the module architecture -- mirrors {@code StudentPaymentController}'s
 * draft/post/void shape exactly; all business logic lives in {@link
 * SalaryService}.
 */
@Controller
@RequestMapping("/salaries")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;
    private final EmployeeService employeeService;
    private final PaymentMethodService paymentMethodService;

    // ---------------------------------------------------------------
    // List / Search / Sort / Paginate
    // ---------------------------------------------------------------

    @GetMapping
    public String list(@ModelAttribute("searchDTO") SalarySearchDTO searchDTO, Model model) {
        PageResponse<SalaryResponseDTO> salariesPage = salaryService.search(searchDTO);
        model.addAttribute("salariesPage", salariesPage);
        model.addAttribute("searchDTO", searchDTO);
        return "salaries/list";
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long employeeId, Model model) {
        SalaryCreateDTO createDTO = new SalaryCreateDTO();
        createDTO.setEmployeeId(employeeId);
        model.addAttribute("createDTO", createDTO);
        populateFormLookups(model, employeeId);
        model.addAttribute("isEdit", false);
        return "salaries/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("createDTO") SalaryCreateDTO createDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormLookups(model, createDTO.getEmployeeId());
            model.addAttribute("isEdit", false);
            return "salaries/form";
        }
        try {
            SalaryResponseDTO created = salaryService.create(createDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Salary " + created.getSalaryNumber() + " was created as a draft.");
            return "redirect:/salaries/" + created.getId();
        } catch (DuplicateResourceException | BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormLookups(model, createDTO.getEmployeeId());
            model.addAttribute("isEdit", false);
            return "salaries/form";
        }
    }

    // ---------------------------------------------------------------
    // View
    // ---------------------------------------------------------------

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("salary", salaryService.getById(id));
        return "salaries/view";
    }

    // ---------------------------------------------------------------
    // Edit Draft
    // ---------------------------------------------------------------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SalaryResponseDTO existing = salaryService.getById(id);
        if (existing.getPaymentStatus() != SalaryPaymentStatus.DRAFT) {
            throw new BusinessValidationException(
                    "Salary " + existing.getSalaryNumber() + " can no longer be edited because it is "
                            + existing.getPaymentStatus() + ", not DRAFT.");
        }

        SalaryUpdateDTO updateDTO = new SalaryUpdateDTO();
        updateDTO.setBasicSalary(existing.getBasicSalary());
        updateDTO.setCurrency(existing.getCurrency());
        updateDTO.setExchangeRateToUsd(existing.getExchangeRateToUsd());
        updateDTO.setBonus(existing.getBonus());
        updateDTO.setOvertimeAmount(existing.getOvertimeAmount());
        updateDTO.setAllowance(existing.getAllowance());
        updateDTO.setPenalty(existing.getPenalty());
        updateDTO.setLoanDeduction(existing.getLoanDeduction());
        updateDTO.setAdvanceDeduction(existing.getAdvanceDeduction());
        updateDTO.setOtherDeduction(existing.getOtherDeduction());
        updateDTO.setPaymentMethodId(existing.getPaymentMethod().getId());
        updateDTO.setRemarks(existing.getRemarks());

        model.addAttribute("salaryId", id);
        model.addAttribute("salary", existing);
        model.addAttribute("updateDTO", updateDTO);
        populateFormLookups(model, existing.getEmployeeId());
        model.addAttribute("isEdit", true);
        return "salaries/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("updateDTO") SalaryUpdateDTO updateDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("salaryId", id);
            model.addAttribute("salary", salaryService.getById(id));
            populateFormLookups(model, null);
            model.addAttribute("isEdit", true);
            return "salaries/form";
        }
        try {
            SalaryResponseDTO updated = salaryService.update(id, updateDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Salary " + updated.getSalaryNumber() + " was updated.");
            return "redirect:/salaries/" + id;
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("salaryId", id);
            model.addAttribute("salary", salaryService.getById(id));
            populateFormLookups(model, null);
            model.addAttribute("isEdit", true);
            return "salaries/form";
        }
    }

    // ---------------------------------------------------------------
    // Post / Void
    // ---------------------------------------------------------------

    @PostMapping("/{id}/post")
    public String post(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            SalaryResponseDTO posted = salaryService.post(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Salary " + posted.getSalaryNumber() + " was posted to the ledger.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/salaries/" + id;
    }

    @PostMapping("/{id}/void")
    public String voidSalary(@PathVariable Long id,
                              @RequestParam(required = false) String reason,
                              RedirectAttributes redirectAttributes) {
        try {
            SalaryResponseDTO voided = salaryService.voidSalary(id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Salary " + voided.getSalaryNumber() + " was voided.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/salaries/" + id;
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
