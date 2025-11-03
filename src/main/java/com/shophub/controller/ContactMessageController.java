package com.shophub.controller;

import com.shophub.model.ContactMessage;
import com.shophub.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact-messages")
@CrossOrigin(origins = "http://localhost:3000")
public class ContactMessageController {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    // Public endpoint - anyone can submit
    @PostMapping
    public ResponseEntity<ContactMessage> submitMessage(@RequestBody ContactMessage message) {
        ContactMessage savedMessage = contactMessageRepository.save(message);
        return ResponseEntity.ok(savedMessage);
    }

    // Admin only - get all messages
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContactMessage>> getAllMessages() {
        List<ContactMessage> messages = contactMessageRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(messages);
    }

    // Admin only - get unread count
    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getUnreadCount() {
        Long count = contactMessageRepository.countByIsReadFalse();
        return ResponseEntity.ok(count);
    }

    // Admin only - mark as read
    @PutMapping("/{id}/mark-read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContactMessage> markAsRead(@PathVariable Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setRead(true);
        ContactMessage updated = contactMessageRepository.save(message);
        return ResponseEntity.ok(updated);
    }

    // Admin only - delete message
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        contactMessageRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
