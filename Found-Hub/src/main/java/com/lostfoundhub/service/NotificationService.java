package com.lostfoundhub.service;

import com.lostfoundhub.dto.NotificationResponse;
import com.lostfoundhub.dto.PageResponse;
import com.lostfoundhub.enums.UserRole;
import com.lostfoundhub.exception.ResourceNotFoundException;
import com.lostfoundhub.model.Notification;
import com.lostfoundhub.model.User;
import com.lostfoundhub.repository.NotificationRepository;
import com.lostfoundhub.repository.UserRepository;
import com.lostfoundhub.util.PageUtil;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /** Joins the caller's transaction, so a notification is saved only if the main action succeeds. */
    @Transactional
    public void notifyUser(User user, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message.length() > 500 ? message.substring(0, 497) + "..." : message);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyAdmins(String message) {
        for (User admin : userRepository.findByRole(UserRole.ADMIN)) {
            notifyUser(admin, message);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Long userId, int page, int size) {
        return PageResponse.from(notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageUtil.of(page, size, Sort.unsorted()))
                .map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + notificationId));
        notification.setRead(true);
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }
}
