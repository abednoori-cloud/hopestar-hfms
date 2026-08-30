package com.hopestar.hfms.module.finance.advance.controller;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.finance.advance.dto.AdvanceRepaymentCreateDTO;
import com.hopestar.hfms.module.finance.advance.dto.AdvanceRepaymentResponseDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceCreateDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceResponseDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceSearchDTO;
import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceUpdateDTO;
import com.hopestar.hfms.module.finance.advance.entity.AdvanceStatus;
import com.hopestar.hfms.module.finance.advance.service.EmployeeAdvanceService;
import com.hopestar.hfms.module.finance.employee.service.EmployeeService;
import com.hopestar.hfms.module.finance.ledger.service.PaymentMethodService;
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
 * MVC controller for the Employee Advances module: list/search/sort/
 * paginate, view, create draft, edit draft, disburse, void, record
 * repayments. Thin per the module architecture -- mirrors {@code
 * EmployeeLoanController}'s draft/action shape exactly; all business
 * logic lives in {@link EmployeeAdvanceService}.
 */
@Controller
@RequestMapping("/advances")
@RequiredArgsConstructor
public class EmployeeAdvanceController {

    private final EmployeeAdvanceService employeeAdvanceService;
    private final EmployeeService employeeService;
    private final PaymentMethodService paymentMethodService;

    // ---------------------------------------------------------------
    // List / Search / Sort / Paginate
    // ---------------------------------------------------------------

    @GetMapping
    public String list(@ModelAttribute("searchDTO") EmployeeAdvanceSearchDTO searchDTO, Model model) {
        PageResponse<EmployeeAdvanceResponseDTO> advancesPage = employeeAdvanceService.search(searchDTO);
        model.addAttribute("advancesPage", advancesPage);
        model.addAttribute("searchDTO", searchDTO);
        return "advances/list";
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long employeeId, Model model) {
        EmployeeAdvanceCreateDTO createDTO = new EmployeeAdvanceCreateDTO();
        createDTO.setEmployeeId(employeeId);
        model.addAttribute("createDTO", createDTO);
        populateFormLookups(model, employeeId);
        model.addAttribute("isEdit", false);
        return "advances/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("createDTO") EmployeeAdvanceCreateDTO createDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormLookups(model, createDTO.getEmployeeId());
            model.addAttribute("isEdit", false);
            return "advances/form";
        }
        try {
            EmployeeAdvanceResponseDTO created = employeeAdvanceService.create(createDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Advance " + created.getAdvanceNumber() + " was created as a draft.");
            return "redirect:/advances/" + created.getId();
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormLookups(model, createDTO.getEmployeeId());
            model.addAttribute("isEdit", false);
            return "advances/form";
        }
    }

    // ---------------------------------------------------------------
    // View
    // ---------------------------------------------------------------

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("advance", employeeAdvanceService.getById(id));
        model.addAttribute("paymentMethods", paymentMethodService.listActive());
        return "advances/view";
    }

    // ---------------------------------------------------------------
    // Edit Draft
    // ---------------------------------------------------------------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        EmployeeAdvanceResponseDTO existing = employeeAdvanceService.getById(id);
        if (existing.getStatus() != AdvanceStatus.DRAFT) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Advance " + existing.getAdvanceNumber() + " can no longer be edited because it is "
                            + existing.getStatus() + ", not DRAFT.");
            return "redirect:/advances/" + id;
        }

        EmployeeAdvanceUpdateDTO updateDTO = new EmployeeAdvanceUpdateDTO();
        updateDTO.setAdvanceAmount(existing.getAdvanceAmount());
        updateDTO.setCurrency(existing.getCurrency());
        updateDTO.setExchangeRateToUsd(existing.getExchangeRateToUsd());
        updateDTO.setAdvanceDate(existing.getAdvanceDate());
        updateDTO.setRemarks(existing.getRemarks());

        model.addAttribute("advanceId", id);
        model.addAttribute("advance", existing);
        model.addAttribute("updateDTO", updateDTO);
        populateFormLookups(model, existing.getEmployeeId());
        model.addAttribute("isEdit", true);
        return "advances/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("updateDTO") EmployeeAdvanceUpdateDTO updateDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("advanceId", id);
            model.addAttribute("advance", employeeAdvanceService.getById(id));
            populateFormLookups(model, null);
            model.addAttribute("isEdit", true);
            return "advances/form";
        }
        try {
            EmployeeAdvanceResponseDTO updated = employeeAdvanceService.update(id, updateDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Advance " + updated.getAdvanceNumber() + " was updated.");
            return "redirect:/advances/" + id;
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("advanceId", id);
            model.addAttribute("advance", employeeAdvanceService.getById(id));
            populateFormLookups(model, null);
            model.addAttribute("isEdit", true);
            return "advances/form";
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
            EmployeeAdvanceResponseDTO disbursed = employeeAdvanceService.disburse(id, paymentMethodId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Advance " + disbursed.getAdvanceNumber() + " was disbursed.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/advances/" + id;
    }

    @PostMapping("/{id}/void")
    public String voidAdvance(@PathVariable Long id,
                               @RequestParam(required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            EmployeeAdvanceResponseDTO voided = employeeAdvanceService.voidAdvance(id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Advance " + voided.getAdvanceNumber() + " was voided.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/advances/" + id;
    }

    // ---------------------------------------------------------------
    // Repayments
    // ---------------------------------------------------------------

    @GetMapping("/{id}/repayments")
    public String repaymentSchedule(@PathVariable Long id, Model model) {
        model.addAttribute("advance", employeeAdvanceService.getById(id));
        model.addAttribute("repayments", employeeAdvanceService.listRepayments(id));
        return "advances/schedule";
    }

    @PostMapping("/{id}/repayments")
    public String recordRepayment(@PathVariable Long id,
                                   @Valid @ModelAttribute("repaymentCreateDTO") AdvanceRepaymentCreateDTO createDTO,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please correct the repayment details and try again.");
            return "redirect:/advances/" + id + "/repayments";
        }
        try {
            AdvanceRepaymentResponseDTO repayment = employeeAdvanceService.recordRepayment(id, createDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Repayment of " + repayment.getCurrency() + " " + repayment.getAmount()
                            + " was recorded against advance " + repayment.getAdvanceNumber() + ".");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/advances/" + id + "/repayments";
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
