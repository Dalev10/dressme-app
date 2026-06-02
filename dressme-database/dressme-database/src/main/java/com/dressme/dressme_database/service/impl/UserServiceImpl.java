package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.model.Provider;
import com.dressme.dressme_database.model.User;
import com.dressme.dressme_database.model.UserIdentity;
import com.dressme.dressme_database.model.UserTasteProfile;
import com.dressme.dressme_database.repository.ProviderRepository;
import com.dressme.dressme_database.repository.UserIdentityRepository;
import com.dressme.dressme_database.repository.UserRepository;
import com.dressme.dressme_database.repository.UserTasteProfileRepository;
import com.dressme.dressme_database.schema.dto.InternalUserCreateRequest;
import com.dressme.dressme_database.schema.dto.UserProfileResponse;
import com.dressme.dressme_database.schema.dto.UserResponseDTO;
import com.dressme.dressme_database.schema.dto.UserUpdateRequest;
import com.dressme.dressme_database.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final ProviderRepository providerRepository;
    private final UserTasteProfileRepository userTasteProfileRepository;

    @Override
    @Transactional
    public UserProfileResponse createUserFromOAuth(InternalUserCreateRequest request) {

        Provider providerEntity = providerRepository.findByName(request.provider().toUpperCase())
                .orElseThrow(() -> new RuntimeException(
                        "Proveedor no soportado o no encontrado: " + request.provider()
                ));

        Optional<UserIdentity> existingIdentity = userIdentityRepository
                .findByProviderAndProviderUserId(providerEntity, request.providerId());

        if (existingIdentity.isPresent()) {
            return mapToResponse(existingIdentity.get().getUser());
        }

        User newUser = User.builder()
                .email(request.email())
                .displayName(request.displayName())
                .profilePicture(request.profilePictureUrl())
                .build();

        newUser = userRepository.save(newUser);

        UserIdentity identity = UserIdentity.builder()
                .user(newUser)
                .provider(providerEntity)
                .providerUserId(request.providerId())
                .build();

        userIdentityRepository.save(identity);

        UserTasteProfile tasteProfile = UserTasteProfile.builder()
                .user(newUser)
                .tasteVector(request.initialTasteVector())
                .sourceType("COLD_START")
                .isCalibrated(false)
                .build();

        userTasteProfileRepository.save(tasteProfile);

        return mapToResponse(newUser);
    }

        private UserProfileResponse mapToResponse(User user) {
                boolean calibrated = userTasteProfileRepository.findByUserId(user.getId())
                        .map(UserTasteProfile::isCalibrated)
                        .orElse(false);

        return new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
            user.getDisplayName(),
            user.getProfilePicture(),
            calibrated 
                );
        }

    @Override
    public UserResponseDTO getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado con ID: " + userId
                ));

        float[] tasteVector = userTasteProfileRepository.findByUserId(userId)
                .map(UserTasteProfile::getTasteVector)
                .orElse(null);

        boolean calibrated = userTasteProfileRepository.findByUserId(userId)
                .map(UserTasteProfile::isCalibrated)
                .orElse(false);

        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .profilePicture(user.getProfilePicture())
                .tasteVector(tasteVector)
                .isCalibrated(calibrated)
                .build();
    }

    @Override
    @Transactional
    public UserResponseDTO updateUserProfile(UUID userId, UserUpdateRequest request) {
        log.info(
                "Database Service: updateUserProfile recibido. userId={}, displayName={}, profilePicture={}, isCalibrated={}, tasteVectorLength={}, sourceType={}",
                userId,
                request.getDisplayName(),
                request.getProfilePicture(),
                request.getIsCalibrated(),
                request.getTasteVector() != null ? request.getTasteVector().length : null,
                request.getSourceType()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }

        if (request.getProfilePicture() != null && !request.getProfilePicture().isBlank()) {
            user.setProfilePicture(request.getProfilePicture());
        }

        userRepository.save(user);

        UserTasteProfile profile = userTasteProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Perfil IA no encontrado para usuario con ID: " + userId
                ));

        log.info(
                "Database Service: Perfil antes de aplicar cambios. isCalibrated={}, sourceType={}, tasteVectorLength={}",
                profile.isCalibrated(),
                profile.getSourceType(),
                profile.getTasteVector() != null ? profile.getTasteVector().length : null
        );

        boolean profileUpdated = false;

        if (request.getIsCalibrated() != null) {
            profile.setCalibrated(request.getIsCalibrated());
            profileUpdated = true;
        }

        if (request.getTasteVector() != null && request.getTasteVector().length > 0) {
            profile.setTasteVector(request.getTasteVector());
            profileUpdated = true;
        }

        if (request.getSourceType() != null && !request.getSourceType().isBlank()) {
            profile.setSourceType(request.getSourceType().trim().toUpperCase());
            profileUpdated = true;
        }

        if (profileUpdated) {
            profile.setLastUpdated(LocalDateTime.now());
            userTasteProfileRepository.save(profile);
        }

        UserResponseDTO response = getUserProfile(userId);

        log.info(
                "Database Service: Perfil después de guardar. isCalibrated={}, sourceType={}",
                response.isCalibrated(),
                profile.getSourceType()
        );

        return response;
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        log.info("Database Service: Iniciando ELIMINACIÓN TOTAL para ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Usuario no encontrado");
        }

        userTasteProfileRepository.findByUserId(userId).ifPresent(profile -> {
            userTasteProfileRepository.delete(profile);
            log.info("1/3: Perfil de gusto eliminado.");
        });

        userIdentityRepository.deleteByUserId(userId);
        log.info("2/3: Identidades externas (Google) eliminadas.");

        userRepository.deleteById(userId);
        log.info("3/3: Registro maestro tbl_users eliminado. Proceso completo.");
    }
}