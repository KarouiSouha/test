package com.healthapp.auth.repository;

import com.healthapp.auth.entity.DoctorActivationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorActivationRequestRepository extends MongoRepository<DoctorActivationRequest, String> {
    
    List<DoctorActivationRequest> findByIsPendingTrue();
    
    Optional<DoctorActivationRequest> findByDoctorId(String doctorId);
    
    List<DoctorActivationRequest> findByProcessedBy(String processedBy); // Changed from findByAdminId
    
    long countByIsPendingTrue();
}