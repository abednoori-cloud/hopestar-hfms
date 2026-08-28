package com.hopestar.hfms.module.finance.employee.controller;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.DuplicateResourceException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeCreateDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeResponseDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeSearchDTO;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeUpdateDTO;
import com.hopestar.hfms.module.finance.employee.service.EmployeeService;
import com.hopestar.hfms.module.finance.employee.service.EmployeeStatusService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC controller for the Employee Management module (Phase 3C): list/
 * search/sort/paginate, view, create, edit, deactivate, reactivate. Thin
 * per the module architecture -- mirrors {@code StudentController} in the
 * Student module exactly; all business logic lives in {@link
 * EmployeeService}.
 */
@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeStatusService employeeStatusService;

    // ---------------------------------------------------------------
    // List / Search / Sort / Paginate
    // ---------------------------------------------------------------

    @GetMapping
    public String list(@ModelAttribute("searchDTO") EmployeeSearchDTO searchDTO, Model model) {
        PageResponse<EmployeeResponseDTO> employeesPage = employeeService.search(searchDTO);
        model.addAttribute("employeesPage", employeesPage);
        model.addAttribute("searchDTO", searchDTO);
        model.addAttribute("statuses", employeeStatusService.listActive());
        return "employees/list";
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("employeeCreateDTO", new EmployeeCreateDTO());
        populateFormLookups(model, false);
        return "employees/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("employeeCreateDTO") EmployeeCreateDTO createDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormLookups(model, false);
            return "employees/form";
        }
        try {
            EmployeeResponseDTO created = employeeService.create(createDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Employee " + created.getEmployeeCode() + " was created successfully.");
            return "redirect:/employees/" + created.getId();
        } catch (DuplicateResourceException | BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormLookups(model, false);
            return "employees/form";
        }
    }

    // ---------------------------------------------------------------
    // View
    // ---------------------------------------------------------------

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("employee", employeeService.getById(id));
        return "employees/view";
    }

    // ---------------------------------------------------------------
    // Edit
    // ---------------------------------------------------------------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        EmployeeResponseDTO existing = employeeService.getById(id);

        EmployeeUpdateDTO updateDTO = new EmployeeUpdateDTO();
        updateDTO.setFullName(existing.getFullName());
        updateDTO.setFatherName(existing.getFatherName());
        updateDTO.setGender(existing.getGender());
        updateDTO.setDateOfBirth(existing.getDateOfBirth());
        updateDTO.setPhone(existing.getPhone());
        updateDTO.setEmail(existing.getEmail());
        updateDTO.setNationalId(existing.getNationalId());
        updateDTO.setAddress(existing.getAddress());
        updateDTO.setPosition(existing.getPosition());
        updateDTO.setDepartment(existing.getDepartment());
        updateDTO.setJoiningDate(existing.getJoiningDate());
        updateDTO.setEmploymentStatusId(existing.getEmploymentStatus().getId());
        updateDTO.setBaseSalary(existing.getBaseSalary());
        updateDTO.setSalaryCurrency(existing.getSalaryCurrency());
        updateDTO.setExchangeRateToUsd(existing.getExchangeRateToUsd());
        updateDTO.setNotes(existing.getNotes());

        model.addAttribute("employeeId", id);
        model.addAttribute("employeeCode", existing.getEmployeeCode());
        model.addAttribute("employeeUpdateDTO", updateDTO);
        populateFormLookups(model, true);
        return "employees/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("employeeUpdateDTO") EmployeeUpdateDTO updateDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("employeeId", id);
            populateFormLookups(model, true);
            return "employees/form";
        }
        try {
            EmployeeResponseDTO updated = employeeService.update(id, updateDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Employee " + updated.getEmployeeCode() + " was updated successfully.");
            return "redirect:/employees/" + id;
        } catch (DuplicateResourceException | BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("employeeId", id);
            populateFormLookups(model, true);
            return "employees/form";
        }
    }

    // ---------------------------------------------------------------
    // Deactivate / Reactivate
    // ---------------------------------------------------------------

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.deactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee was deactivated.");
        return "redirect:/employees";
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.reactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Employee was reactivated.");
        return "redirect:/employees/" + id;
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void populateFormLookups(Model model, boolean isEdit) {
        model.addAttribute("statuses", employeeStatusService.listActive());
        model.addAttribute("isEdit", isEdit);
    }
}
