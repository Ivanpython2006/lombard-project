package com.lombard.service;

import com.lombard.model.Collateral;
import com.lombard.repository.CollateralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class CollateralService {

    private static final int MAX_PHOTOS = 10;

    private final CollateralRepository collateralRepository;
    private final NumberSequenceService numberSequenceService;
    private final FileStorageService fileStorageService;

    public CollateralService(CollateralRepository collateralRepository,
                             NumberSequenceService numberSequenceService,
                             FileStorageService fileStorageService) {
        this.collateralRepository = collateralRepository;
        this.numberSequenceService = numberSequenceService;
        this.fileStorageService = fileStorageService;
    }

    public Collateral save(Collateral collateral) {
        if (collateral.getItemNumber() == null || collateral.getItemNumber().isBlank()) {
            collateral.setItemNumber(numberSequenceService.nextNumber("M_ITEM", "M"));
        }
        return collateralRepository.save(collateral);
    }

    public Collateral addCollateral(Collateral collateral) {
        return save(collateral);
    }

    public Collateral findById(Long id) {
        return collateralRepository.findById(id).orElse(null);
    }

    public List<Collateral> getCollateralByLoanId(Long loanId) {
        return collateralRepository.findByLoanId(loanId);
    }

    public List<Collateral> findAll() {
        return collateralRepository.findAll();
    }

    public List<Collateral> searchByAnyField(String query) {
        return collateralRepository.searchByAnyField(query);
    }

    @Transactional
    public void delete(Long id) {
        Collateral c = collateralRepository.findById(id).orElse(null);
        if (c != null) {
            List<String> photos = new ArrayList<>(c.getPhotoUrls());
            for (String u : photos) {
                fileStorageService.delete(u);
            }
        }
        collateralRepository.deleteById(id);
    }

    @Transactional
    public void appendPhotos(Collateral collateral, MultipartFile[] newPhotos, String prefix) {
        if (newPhotos == null || newPhotos.length == 0) return;

        List<String> current = collateral.getPhotoUrls();
        if (current == null) current = new ArrayList<>();

        int remaining = MAX_PHOTOS - current.size();
        if (remaining <= 0) {
            throw new IllegalArgumentException(
                    "Достигнут лимит фотографий (" + MAX_PHOTOS + "). Удалите часть, чтобы добавить новые.");
        }

        MultipartFile[] toSave = newPhotos;
        if (newPhotos.length > remaining) {
            toSave = new MultipartFile[remaining];
            System.arraycopy(newPhotos, 0, toSave, 0, remaining);
        }

        List<String> saved = fileStorageService.saveAll(toSave, prefix);
        if (saved.isEmpty()) return;

        current.addAll(saved);
        collateral.setPhotoUrls(current);

        collateralRepository.save(collateral);
    }

    @Transactional
    public void removePhoto(Collateral collateral, String photoUrl) {
        if (photoUrl == null) return;

        List<String> current = collateral.getPhotoUrls();
        if (current == null) return;

        current.remove(photoUrl);
        collateral.setPhotoUrls(current);
        collateralRepository.save(collateral);

        fileStorageService.delete(photoUrl);
    }

    public long count() {
        return collateralRepository.count();
    }
}