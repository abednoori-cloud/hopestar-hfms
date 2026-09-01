package com.hopestar.hfms.module.auth.controller;

import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.service.LogoService;
import com.hopestar.hfms.module.auth.dto.BranchResponseDTO;
import com.hopestar.hfms.module.auth.dto.OrganizationSettingsUpdateDTO;
import com.hopestar.hfms.module.auth.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC controller for the Organization Settings page: edits the single
 * headquarters {@code Branch} record in place (name/address/phone/email/
 * website/logo) rather than a separate Settings entity -- see {@link
 * BranchService}'s Javadoc. Thin per the module architecture; {@link
 * BranchService} owns the actual mutation and the {@code SETTINGS_MANAGE}
 * authorization check.
 */
@Controller
@RequestMapping("/settings/organization")
@RequiredArgsConstructor
public class OrganizationSettingsController {

    private final BranchService branchService;
    private final LogoService logoService;

    @GetMapping
    public String edit(Model model) {
        BranchResponseDTO headquarters = branchService.getHeadquarters();
        populateForm(model, headquarters);
        return "settings/organization";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("updateDTO") OrganizationSettingsUpdateDTO updateDTO,
                          BindingResult bindingResult,
                          @RequestPart(value = "logo", required = false) MultipartFile logo,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentLogoDataUri", logoService.getLogoDataUri());
            return "settings/organization";
        }
        try {
            branchService.updateOrganizationSettings(updateDTO, logo);
            redirectAttributes.addFlashAttribute("successMessage", "Organization settings were updated.");
        } catch (BusinessValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings/organization";
    }

    private void populateForm(Model model, BranchResponseDTO headquarters) {
        OrganizationSettingsUpdateDTO updateDTO = new OrganizationSettingsUpdateDTO();
        updateDTO.setName(headquarters.getName());
        updateDTO.setAddress(headquarters.getAddress());
        updateDTO.setPhone(headquarters.getPhone());
        updateDTO.setEmail(headquarters.getEmail());
        updateDTO.setWebsite(headquarters.getWebsite());

        model.addAttribute("updateDTO", updateDTO);
        model.addAttribute("headquarters", headquarters);
        model.addAttribute("currentLogoDataUri", logoService.getLogoDataUri());
    }
}
