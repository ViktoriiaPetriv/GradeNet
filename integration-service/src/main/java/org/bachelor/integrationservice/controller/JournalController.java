package org.bachelor.integrationservice.controller;

import lombok.RequiredArgsConstructor;
import org.bachelor.integrationservice.model.dto.JournalDisciplineStatusDTO;
import org.bachelor.integrationservice.model.dto.JournalImportRequestDTO;
import org.bachelor.integrationservice.model.dto.JournalImportResultDTO;
import org.bachelor.integrationservice.model.dto.JournalStudentStatusDTO;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDTO;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDetailDTO;
import org.bachelor.integrationservice.model.journal.JournalSpecialtyDTO;
import org.bachelor.integrationservice.model.journal.JournalStudentDTO;
import org.bachelor.integrationservice.service.JournalImportService;
import org.bachelor.integrationservice.service.journal.JournalClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
public class JournalController {

    private final JournalClient journalClient;
    private final JournalImportService journalImportService;

    @GetMapping("/specialties")
    public List<JournalSpecialtyDTO> getSpecialties(
            @RequestParam(required = false) String degree,
            @RequestParam(required = false) Integer graduationYear,
            @RequestParam(required = false) String studyForm,
            @RequestParam(required = false) String code) {
        return journalClient.getSpecialties(degree, graduationYear, studyForm, code);
    }

    @GetMapping("/disciplines/{specialtyId}")
    public List<JournalDisciplineDTO> getDisciplines(@PathVariable long specialtyId) {
        return journalClient.getDisciplines(specialtyId);
    }

    @GetMapping("/disciplines-status/{specialtyId}")
    public List<JournalDisciplineStatusDTO> getDisciplinesWithStatus(
            @PathVariable long specialtyId,
            @RequestHeader("Authorization") String authHeader) {
        return journalImportService.getDisciplinesWithStatus(specialtyId, authHeader);
    }

    @GetMapping("/students/{specialtyId}")
    public List<JournalStudentDTO> getStudents(@PathVariable long specialtyId) {
        return journalClient.getStudents(specialtyId);
    }

    @GetMapping("/students-status/{specialtyId}")
    public List<JournalStudentStatusDTO> getStudentsWithStatus(
            @PathVariable long specialtyId,
            @RequestHeader("Authorization") String authHeader) {
        return journalImportService.getStudentsWithStatus(specialtyId, authHeader);
    }

    @GetMapping("/discipline/{disciplineId}")
    public JournalDisciplineDetailDTO getDisciplineDetail(@PathVariable long disciplineId) {
        return journalClient.getDisciplineDetail(disciplineId);
    }

    @PostMapping("/import")
    public JournalImportResultDTO importFromJournal(
            @RequestBody JournalImportRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {
        return journalImportService.importFromJournal(request, authHeader);
    }
}
