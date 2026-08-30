package com.hopestar.hfms.module.student.controller;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.DuplicateResourceException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.student.dto.StudentContractCreateDTO;
import com.hopestar.hfms.module.student.dto.StudentCreateDTO;
import com.hopestar.hfms.module.student.dto.StudentDocumentResponseDTO;
import com.hopestar.hfms.module.student.dto.StudentDocumentUploadDTO;
import com.hopestar.hfms.module.student.dto.StudentResponseDTO;
import com.hopestar.hfms.module.student.dto.StudentSearchDTO;
import com.hopestar.hfms.module.student.dto.StudentUpdateDTO;
import com.hopestar.hfms.module.student.service.ProgramService;
import com.hopestar.hfms.module.student.service.StudentContractService;
import com.hopestar.hfms.module.student.service.StudentDocumentService;
import com.hopestar.hfms.module.student.service.StudentService;
import com.hopestar.hfms.module.student.service.StudentStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Path;

/**
 * MVC controller for the Student Management module (SRS Module 3):
 * list/search, view, create, edit, deactivate, contract management, and
 * document upload/download. Thin per the module architecture — all
 * business logic and validation lives in the Service layer; this class
 * only binds requests, resolves view names, and carries flash messages
 * across redirects (post/redirect/get).
 */
@Slf4j
@Controller
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentContractService studentContractService;
    private final StudentDocumentService studentDocumentService;
    private final ProgramService programService;
    private final StudentStatusService studentStatusService;

    // ---------------------------------------------------------------
    // List / Search
    // ---------------------------------------------------------------

    @GetMapping
    public String list(@ModelAttribute("searchDTO") StudentSearchDTO searchDTO, Model model) {
        PageResponse<StudentResponseDTO> studentsPage = studentService.search(searchDTO);
        model.addAttribute("studentsPage", studentsPage);
        model.addAttribute("searchDTO", searchDTO);
        model.addAttribute("programs", programService.listActive());
        model.addAttribute("statuses", studentStatusService.listActive());
        return "students/list";
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("studentCreateDTO", new StudentCreateDTO());
        model.addAttribute("statuses", studentStatusService.listActive());
        model.addAttribute("isEdit", false);
        return "students/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("studentCreateDTO") StudentCreateDTO createDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormLookups(model, false);
            return "students/form";
        }
        try {
            StudentResponseDTO created = studentService.create(createDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Student " + created.getStudentCode() + " was created successfully.");
            return "redirect:/students/" + created.getId();
        } catch (DuplicateResourceException | BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormLookups(model, false);
            return "students/form";
        }
    }

    // ---------------------------------------------------------------
    // View
    // ---------------------------------------------------------------

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("student", studentService.getById(id));
        return "students/view";
    }

    // ---------------------------------------------------------------
    // Edit
    // ---------------------------------------------------------------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        StudentResponseDTO existing = studentService.getById(id);

        StudentUpdateDTO updateDTO = new StudentUpdateDTO();
        updateDTO.setFullName(existing.getFullName());
        updateDTO.setFatherName(existing.getFatherName());
        updateDTO.setPhone(existing.getPhone());
        updateDTO.setEmail(existing.getEmail());
        updateDTO.setPassportNumber(existing.getPassportNumber());
        updateDTO.setPassportExpiry(existing.getPassportExpiry());
        updateDTO.setProgramName(existing.getProgram().getName());
        updateDTO.setDestinationCountry(existing.getDestinationCountry());
        updateDTO.setStatusId(existing.getStatus().getId());
        updateDTO.setRegistrationDate(existing.getRegistrationDate());
        updateDTO.setNotes(existing.getNotes());

        model.addAttribute("studentId", id);
        model.addAttribute("studentCode", existing.getStudentCode());
        model.addAttribute("studentUpdateDTO", updateDTO);
        populateFormLookups(model, true);
        return "students/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("studentUpdateDTO") StudentUpdateDTO updateDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("studentId", id);
            populateFormLookups(model, true);
            return "students/form";
        }
        try {
            StudentResponseDTO updated = studentService.update(id, updateDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Student " + updated.getStudentCode() + " was updated successfully.");
            return "redirect:/students/" + id;
        } catch (DuplicateResourceException | BusinessValidationException | ResourceNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("studentId", id);
            populateFormLookups(model, true);
            return "students/form";
        }
    }

    // ---------------------------------------------------------------
    // Deactivate / Reactivate
    // ---------------------------------------------------------------

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        studentService.deactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Student was deactivated.");
        return "redirect:/students";
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        studentService.reactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Student was reactivated.");
        return "redirect:/students/" + id;
    }

    // ---------------------------------------------------------------
    // Contracts
    // ---------------------------------------------------------------

    @GetMapping("/{id}/contracts")
    public String contracts(@PathVariable Long id, Model model) {
        model.addAttribute("student", studentService.getById(id));
        model.addAttribute("contracts", studentContractService.listByStudent(id));
        model.addAttribute("contractCreateDTO", new StudentContractCreateDTO());
        model.addAttribute("programs", programService.listActive());
        return "students/contracts";
    }

    @PostMapping("/{id}/contracts")
    public String createContract(@PathVariable Long id,
                                  @Valid @ModelAttribute("contractCreateDTO") StudentContractCreateDTO createDTO,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("student", studentService.getById(id));
            model.addAttribute("contracts", studentContractService.listByStudent(id));
            model.addAttribute("programs", programService.listActive());
            return "students/contracts";
        }
        try {
            studentContractService.create(id, createDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Contract was added successfully.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/students/" + id + "/contracts";
    }

    @PostMapping("/{id}/contracts/{contractId}/cancel")
    public String cancelContract(@PathVariable Long id, @PathVariable Long contractId,
                                  RedirectAttributes redirectAttributes) {
        try {
            studentContractService.cancel(contractId);
            redirectAttributes.addFlashAttribute("successMessage", "Contract was cancelled.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/students/" + id + "/contracts";
    }

    @PostMapping("/{id}/contracts/{contractId}/complete")
    public String completeContract(@PathVariable Long id, @PathVariable Long contractId,
                                    RedirectAttributes redirectAttributes) {
        try {
            studentContractService.complete(contractId);
            redirectAttributes.addFlashAttribute("successMessage", "Contract was marked completed.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/students/" + id + "/contracts";
    }

    // ---------------------------------------------------------------
    // Documents
    // ---------------------------------------------------------------

    @GetMapping("/{id}/documents")
    public String documents(@PathVariable Long id, Model model) {
        model.addAttribute("student", studentService.getById(id));
        model.addAttribute("documents", studentDocumentService.listByStudent(id));
        model.addAttribute("uploadDTO", new StudentDocumentUploadDTO());
        return "students/documents";
    }

    @PostMapping("/{id}/documents")
    public String uploadDocument(@PathVariable Long id,
                                  @Valid @ModelAttribute("uploadDTO") StudentDocumentUploadDTO uploadDTO,
                                  BindingResult bindingResult,
                                  @RequestPart("file") MultipartFile file,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("student", studentService.getById(id));
            model.addAttribute("documents", studentDocumentService.listByStudent(id));
            return "students/documents";
        }
        try {
            studentDocumentService.upload(id, uploadDTO, file);
            redirectAttributes.addFlashAttribute("successMessage", "Document uploaded successfully.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/students/" + id + "/documents";
    }

    @PostMapping("/{id}/documents/{documentId}/deactivate")
    public String deactivateDocument(@PathVariable Long id, @PathVariable Long documentId,
                                      RedirectAttributes redirectAttributes) {
        studentDocumentService.deactivate(documentId);
        redirectAttributes.addFlashAttribute("successMessage", "Document was removed.");
        return "redirect:/students/" + id + "/documents";
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @PathVariable Long documentId) {
        StudentDocumentResponseDTO documentMeta = studentDocumentService.getById(documentId);
        if (!documentMeta.getStudentId().equals(id)) {
            throw new ResourceNotFoundException("Student document", documentId);
        }
        String storedPath = studentDocumentService.resolveFilePath(documentId);
        File file = Path.of(storedPath).toFile();
        if (!file.exists()) {
            throw new ResourceNotFoundException("Stored file", documentId);
        }
        Resource resource = new FileSystemResource(file);
        String contentType = documentMeta.getContentType() != null
                ? documentMeta.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String downloadName = documentMeta.getOriginalFileName() != null
                ? documentMeta.getOriginalFileName() : "document";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .body(resource);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void populateFormLookups(Model model, boolean isEdit) {
        model.addAttribute("statuses", studentStatusService.listActive());
        model.addAttribute("isEdit", isEdit);
    }
}
