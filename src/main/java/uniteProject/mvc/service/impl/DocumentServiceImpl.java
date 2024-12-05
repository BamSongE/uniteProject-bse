package uniteProject.mvc.service.impl;

import lombok.RequiredArgsConstructor;
import uniteProject.global.Protocol;
import uniteProject.mvc.model.Application;
import uniteProject.mvc.model.Student;
import uniteProject.mvc.model.TBCertificate;
import uniteProject.mvc.repository.ApplicationRepository;
import uniteProject.mvc.repository.DocumentRepository;
import uniteProject.mvc.repository.StudentRepository;
import uniteProject.mvc.service.interfaces.DocumentService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    public Protocol submitTBCertificate(byte[] data) {
        Protocol response = new Protocol(Protocol.TYPE_RESPONSE, Protocol.CODE_SUCCESS);

        try {
            // data format: "studentNumber,imageData"
            String studentNumber = new String(data, 0, 9, StandardCharsets.UTF_8);
            byte[] imageData = new byte[data.length - 10];
            System.arraycopy(data, 10, imageData, 0, data.length - 10);

            Student student = studentRepository.findByStudentNumber(studentNumber)
                    .orElseThrow(() -> new RuntimeException("학생 정보를 찾을 수 없습니다."));

            Application application = applicationRepository.findByStudentId(student.getId())
                    .orElseThrow(() -> new RuntimeException("신청 정보를 찾을 수 없습니다."));

            // 이미 제출된 결핵진단서가 있는지 확인
            if (documentRepository.existsByApplicationId(application.getId())) {
                response.setCode(Protocol.CODE_FAIL);
                response.setData("이미 제출된 결핵진단서가 있습니다.");
                return response;
            }

            TBCertificate certificate = TBCertificate.builder()
                    .applicationId(application.getId())
                    .image(imageData)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            documentRepository.save(certificate);
            response.setData("결핵진단서가 성공적으로 제출되었습니다.");

        } catch (Exception e) {
            response.setCode(Protocol.CODE_FAIL);
            response.setData("결핵진단서 제출 중 오류가 발생했습니다: " + e.getMessage());
        }

        return response;
    }

    @Override
    public Protocol checkSubmissionStatus(byte[] data) {
        Protocol response = new Protocol(Protocol.TYPE_RESPONSE, Protocol.CODE_SUCCESS);

        try {
            String studentNumber = new String(data, StandardCharsets.UTF_8);
            Student student = studentRepository.findByStudentNumber(studentNumber)
                    .orElseThrow(() -> new RuntimeException("학생 정보를 찾을 수 없습니다."));

            Application application = applicationRepository.findByStudentId(student.getId())
                    .orElseThrow(() -> new RuntimeException("신청 정보를 찾을 수 없습니다."));

            Optional<TBCertificate> certificate = documentRepository.findByApplicationId(application.getId());

            if (certificate.isPresent()) {
                String submissionInfo = String.format("제출완료,제출시간:%s",
                        certificate.get().getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                response.setData(submissionInfo);
            } else {
                response.setData("미제출");
            }

        } catch (Exception e) {
            response.setCode(Protocol.CODE_FAIL);
            response.setData("제출 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
        }

        return response;
    }
}