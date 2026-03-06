package in.dhruvpathak.cloudshareapi.repository;

import in.dhruvpathak.cloudshareapi.document.ProfileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProfileRepository extends MongoRepository<ProfileDocument,String> {
    Optional<ProfileDocument> findByEmail(String email);

    ProfileDocument findByClerkId(String clerkId);

    boolean existsByClerkId(String clerkId);

}
