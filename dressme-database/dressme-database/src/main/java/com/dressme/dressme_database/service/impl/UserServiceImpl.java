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
        
        // 1. Buscar la entidad Provider maestra en la base de datos
        Provider providerEntity = providerRepository.findByName(request.provider().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Proveedor no soportado o no encontrado: " + request.provider()));

        // 2. Validar si la identidad ya existe (Flujo de Login vs Registro)
        Optional<UserIdentity> existingIdentity = userIdentityRepository
                .findByProviderAndProviderUserId(providerEntity, request.providerId());

        if (existingIdentity.isPresent()) {
            return mapToResponse(existingIdentity.get().getUser());
        }

        // 3. Crear y persistir el Usuario base
        User newUser = User.builder()
                .email(request.email())
                .displayName(request.displayName())
                .profilePicture(request.profilePictureUrl())
                .build();
        newUser = userRepository.save(newUser);

        // 4. Crear y persistir el vínculo de Identidad
        UserIdentity identity = UserIdentity.builder()
                .user(newUser)
                .provider(providerEntity)
                .providerUserId(request.providerId())
                .build();
        userIdentityRepository.save(identity);

        // 5. Crear y persistir el perfil IA con el vector inicial
        UserTasteProfile tasteProfile = UserTasteProfile.builder()
                .user(newUser)
                .tasteVector(request.initialTasteVector())
                .sourceType("COLD_START")
                .isCalibrated(false)
                .build();
        userTasteProfileRepository.save(tasteProfile);

        // 6. Retornar el DTO limpio
        return mapToResponse(newUser);
    }

    // Método auxiliar privado para centralizar el mapeo de la respuesta
    private UserProfileResponse mapToResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProfilePicture(),
                false // isCalibrated por defecto en false durante el registro (Cold Start)
        );
    }

    @Override
    public UserResponseDTO getUserProfile(UUID userId) {
        // 1. Buscamos al usuario base. Si no existe, lanzamos error 404
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        // 2. Buscamos su estado de calibración en la otra tabla
        boolean calibrated = userTasteProfileRepository.findByUserId(userId)
                .map(UserTasteProfile::isCalibrated)
                .orElse(false);

        // 3. Construimos el DTO de salida
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .profilePicture(user.getProfilePicture())
                .isCalibrated(calibrated)
                .build();
    }

    @Override
    @Transactional // CRUCIAL: Garantiza que se actualicen ambas tablas o ninguna
    public UserResponseDTO updateUserProfile(UUID userId, UserUpdateRequest request) {
        log.info("Database Service: Iniciando actualización para usuario ID: {}", userId);

        // 1. Buscar y actualizar Entidad User (tbl_users)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getProfilePicture() != null) {
            user.setProfilePicture(request.getProfilePicture());
        }
        userRepository.save(user);

        // 2. Buscar y actualizar Entidad UserTasteProfile (tbl_user_taste_profile)
        userTasteProfileRepository.findByUserId(userId).ifPresent(profile -> {
            if (request.getIsCalibrated() != null) {
                profile.setCalibrated(request.getIsCalibrated());
            }
            if (request.getTasteVector() != null) {
                profile.setTasteVector(request.getTasteVector());
            }
            profile.setLastUpdated(java.time.LocalDateTime.now());
            userTasteProfileRepository.save(profile);
        });

        // 3. Reutilizamos el método GET funcional para retornar el estado final íntegro
        return getUserProfile(userId);
    }

    @Override
    @Transactional // CRÍTICO: Todo el borrado ocurre en una sola unidad de trabajo
    public void deleteUser(UUID userId) {
        log.info("Database Service: Iniciando ELIMINACIÓN TOTAL para ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Usuario no encontrado");
        }

        // 1. Borrar Perfil de Gustos (Hijo)
        userTasteProfileRepository.findByUserId(userId).ifPresent(profile -> {
            userTasteProfileRepository.delete(profile);
            log.info("1/3: Perfil de gusto eliminado.");
        });

        // 2. Borrar Identidades de Auth (Hijo) - SOLUCIONA EL ERROR SQL
        userIdentityRepository.deleteByUserId(userId);
        log.info("2/3: Identidades externas (Google) eliminadas.");

        // 3. Borrar el Usuario (Padre)
        userRepository.deleteById(userId);
        log.info("3/3: Registro maestro tbl_users eliminado. Proceso completo.");
    }
}