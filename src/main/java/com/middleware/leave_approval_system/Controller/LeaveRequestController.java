package com.middleware.leave_approval_system.Controller;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobProperties;
import com.middleware.leave_approval_system.Entity.LeaveRequest;
import com.middleware.leave_approval_system.Exception.ResourceNotFoundException;
import com.middleware.leave_approval_system.Service.LeaveRequestServiceImpl;
import com.middleware.leave_approval_system.Util.HolidaysUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leave")
public class LeaveRequestController {

    @Autowired
    private LeaveRequestServiceImpl leaveRequestServiceImpl;

    @Value("${azure.storage.blob.connection-string}")
    private String blobConnectionString;

    @Value("${azure.storage.blob.container-name}")
    private String blobContainerName;

    @PostMapping(value = "/submit", produces = "application/json")
    public ResponseEntity<?> submitLeaveRequest(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("position") String position,
            @RequestParam("phone") String phone,
            @RequestParam("managerId") String managerId,
            @RequestParam("managerName") String managerName,
            @RequestParam("managerEmail") String managerEmail,
            @RequestParam(value = "comments", required = false) String comments,
            @RequestParam(value = "durationType", required = false) String durationType,
            @RequestParam(value = "duration", required = false) Double duration,
            @RequestParam("leaveStartDate") LocalDate leaveStartDate,
            @RequestParam("leaveEndDate") LocalDate leaveEndDate,
            @RequestParam(value = "leaveReason", required = false) String leaveReason,
            @RequestParam(value = "leaveStatus", required = false) LeaveRequest.LeaveStatus leaveStatus,
            @RequestParam(value = "leaveType", required = false) LeaveRequest.LeaveType leaveType,
            @RequestParam(value = "medicalDocument", required = false) MultipartFile medicalDocument) {
        try {
            LeaveRequest leaveRequest = new LeaveRequest();
            leaveRequest.setEmployeeId(employeeId);
            leaveRequest.setFirstName(firstName);
            leaveRequest.setLastName(lastName);
            leaveRequest.setEmail(email);
            leaveRequest.setPosition(position);
            leaveRequest.setPhone(phone);
            leaveRequest.setManagerId(managerId);
            leaveRequest.setManagerName(managerName);
            leaveRequest.setManagerEmail(managerEmail);
            leaveRequest.setComments(comments);
            leaveRequest.setLeaveType(leaveType);
            leaveRequest.setLeaveStartDate(leaveStartDate);
            leaveRequest.setLeaveEndDate(leaveEndDate);
            leaveRequest.setLeaveReason(leaveReason);
            leaveRequest.setLeaveStatus(leaveStatus);

            if (leaveType == LeaveRequest.LeaveType.SICK) {
                double requestedDays = leaveRequest.calculateBusinessDays(
                        leaveStartDate, leaveEndDate, HolidaysUtil.getNationalHolidays(leaveStartDate.getYear())
                );
                if (requestedDays > 2 && medicalDocument != null) {
                    String savedFilePath = saveFileToBlobStorage(medicalDocument, "medicalDocument");
                    leaveRequest.setMedicalDocument(savedFilePath);
                }
            }

            LeaveRequest savedRequest = leaveRequestServiceImpl.submitLeaveRequest(leaveRequest);
            return new ResponseEntity<>(savedRequest, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>("File upload failed: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    private String saveFileToBlobStorage(MultipartFile file, String fileType) throws IOException {
        if (file != null && !file.isEmpty()) {
            // Build the BlobClient for the target blob
            BlobClient blobClient = new BlobClientBuilder()
                    .connectionString(blobConnectionString)
                    .containerName(blobContainerName)
                    .blobName(fileType + "-" + file.getOriginalFilename())
                    .buildClient();

            // Upload the file to Blob Storage
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            // Return the URL of the uploaded blob
            return blobClient.getBlobUrl();
        }
        return null;
    }

    @PutMapping(value = "/approve/{id}")
    public ResponseEntity<String> approveLeaveRequest(@PathVariable Long id) {
        leaveRequestServiceImpl.approveLeaveRequest(id);
        return ResponseEntity.ok("Leave Request Approved");
    }

    @PutMapping(value = "/reject/{id}/{leaveReason}")
    public ResponseEntity<String> rejectLeaveRequest(@PathVariable Long id, @PathVariable String leaveReason) {
        leaveRequestServiceImpl.rejectLeaveRequest(id, leaveReason);
        return ResponseEntity.ok("Leave Request Rejected with Reason: " + leaveReason);
    }

    @PutMapping(value = "/update/{id}", produces = "application/json")
    public ResponseEntity<LeaveRequest> updateLeaveRequest(@PathVariable Long id, @RequestBody LeaveRequest leaveRequest) {
        LeaveRequest updatedLeaveRequest = leaveRequestServiceImpl.updateLeaveRequest(id, leaveRequest);
        return ResponseEntity.ok(updatedLeaveRequest);
    }

    @DeleteMapping(value = "/delete/{id}")
    public ResponseEntity<String> deleteLeaveRequest(@PathVariable Long id) {
        String deleteRequest = leaveRequestServiceImpl.deleteLeaveRequest(id);
        return ResponseEntity.ok(deleteRequest);
    }

    @GetMapping
    public ResponseEntity<List<LeaveRequest>> getAllLeaveRequests() {
        List<LeaveRequest> leaveRequests = leaveRequestServiceImpl.getAllLeaveRequests();
        if (leaveRequests.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(leaveRequests);
        }
        return new ResponseEntity<>(leaveRequests, HttpStatus.OK);
    }

    @GetMapping(value = "/{status}/manager/{managerId}", produces = "application/json")
    public ResponseEntity<List<LeaveRequest>> getLeaveRequestsByStatus(@PathVariable String status, @PathVariable String managerId) {
        LeaveRequest.LeaveStatus leaveStatus;
        try {
            leaveStatus = LeaveRequest.LeaveStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        List<LeaveRequest> leaveRequests = leaveRequestServiceImpl.getLeaveRequestsByStatus(managerId, leaveStatus);
        if (leaveRequests.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(leaveRequests);
        }
        return ResponseEntity.ok(leaveRequests);
    }

    @GetMapping("/fileSize")
    public ResponseEntity<Map<String, Long>> getFileSize(@RequestParam String fileName) {
        try {
            // Create a BlobServiceClient using the connection string
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(blobConnectionString)
                    .buildClient();

            // Get the container client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(blobContainerName);

            // Check if the blob exists
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            if (blobClient.exists()) {
                // Fetch blob properties to get the file size
                BlobProperties properties = blobClient.getProperties();
                long fileSize = properties.getBlobSize(); // File size in bytes

                // Prepare response
                Map<String, Long> response = new HashMap<>();
                response.put("size", fileSize);
                return ResponseEntity.ok(response);
            } else {
                // If the file does not exist, return size as 0 with an appropriate message
                Map<String, Long> response = new HashMap<>();
                response.put("size", 0L);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            // Handle any errors that occur
            Map<String, Long> response = new HashMap<>();
            response.put("size", 0L); // Error occurred, size is 0
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @GetMapping("/pending/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getAllPendingLeaveRequestsEmployees(String employeeId) {
        List<LeaveRequest> pendingRequests = leaveRequestServiceImpl.getAllPendingLeaveRequestsEmployee(employeeId);
        if (pendingRequests.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pendingRequests);
        }
        return new ResponseEntity<>(pendingRequests, HttpStatus.OK);
    }

    @GetMapping("/approve/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getAllApprovedLeaveRequestsEmployee(String employeeId) {
        List<LeaveRequest> approveRequests = leaveRequestServiceImpl.getAllApprovedLeaveRequestsEmployee(employeeId);
        if (approveRequests.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(approveRequests);
        }
        return new ResponseEntity<>(approveRequests, HttpStatus.OK);
    }

    @GetMapping("/reject/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getAllRejectedLeaveRequestsEmployee(String employeeId) {
        List<LeaveRequest> rejectRequests = leaveRequestServiceImpl.getAllRejectedLeaveRequestsEmployee(employeeId);
        if (rejectRequests.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rejectRequests);
        }
        return new ResponseEntity<>(rejectRequests, HttpStatus.OK);
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<LeaveRequest>> getAllMangerIds(@PathVariable String managerId) {
        List<LeaveRequest> leaveRequest = leaveRequestServiceImpl.getAllManagerId(managerId);
        if (leaveRequest.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(leaveRequest);
        }
        return new ResponseEntity<>(leaveRequest, HttpStatus.OK);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getAllEmployeeIds(@PathVariable String employeeId) {
        List<LeaveRequest> leaveRequest = leaveRequestServiceImpl.getAllEmployeeId(employeeId);
        return ResponseEntity.ok(leaveRequest);
    }
}
