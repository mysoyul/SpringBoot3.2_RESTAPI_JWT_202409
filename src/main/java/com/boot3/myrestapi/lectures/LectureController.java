package com.boot3.myrestapi.lectures;

import com.boot3.myrestapi.commons.exception.BusinessException;
import com.boot3.myrestapi.commons.resource.ErrorsResource;
import com.boot3.myrestapi.lectures.dto.LectureReqDto;
import com.boot3.myrestapi.lectures.dto.LectureResDto;
import com.boot3.myrestapi.lectures.dto.LectureResource;
import com.boot3.myrestapi.lectures.validator.LectureValidator;

import com.boot3.myrestapi.security.userinfos.annot.CurrentUser;
import com.boot3.myrestapi.security.userinfos.domain.UserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping(value = "/api/lectures", produces = MediaTypes.HAL_JSON_VALUE)
@RequiredArgsConstructor
public class LectureController {
    private final LectureRepository lectureRepository;
    private final ModelMapper modelMapper;
    private final LectureValidator lectureValidator;

//    public LectureController(LectureRepository lectureRepository) {
//        this.lectureRepository = lectureRepository;
//        System.out.println("LectureRepository 구현 클래스명" + lectureRepository.getClass().getName());
//    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLecture(@PathVariable Integer id,
                                           @RequestBody @Valid LectureReqDto lectureReqDto,
                                           Errors errors,
                                           @CurrentUser UserInfo currentUser) {

        Lecture existingLecture = getLectureExistOrElseThrow(id);

        if (errors.hasErrors()) {
            return badRequest(errors);
        }

        lectureValidator.validate(lectureReqDto, errors);
        if (errors.hasErrors()) {
            return badRequest(errors);
        }

        //Lecture 가 참조하는 UserInfo 객체와 인증한 UserInfo 객체가 다르면 401 인증 오류
        if((existingLecture.getUserInfo() != null) && (!existingLecture.getUserInfo().equals(currentUser))) {
            throw new AccessDeniedException("등록한 User 와 수정을 요청한 User 가 달라서 수정 권한이 없습니다.");
        }

        this.modelMapper.map(lectureReqDto, existingLecture);
        //free, offline 필드 update
        existingLecture.update();
        //DB 저장
        Lecture savedLecture = this.lectureRepository.save(existingLecture);
        //수정된 Entity => ResDto
        LectureResDto lectureResDto = modelMapper.map(savedLecture, LectureResDto.class);
        //Lecture 객체와 연관된 UserInfo 객체가 있다면 LectureResDto 에 email set
        if(savedLecture.getUserInfo() != null)
            lectureResDto.setEmail(savedLecture.getUserInfo().getEmail());
        LectureResource lectureResource = new LectureResource(lectureResDto);
        return ResponseEntity.ok(lectureResource);
    }

    private Lecture getLectureExistOrElseThrow(Integer id) {
        String errMsg = String.format("Id = %d Lecture Not Found", id);
        return this.lectureRepository.findById(id)
                .orElseThrow(() -> new BusinessException(errMsg, HttpStatus.NOT_FOUND));
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> getLecture(@PathVariable Integer id,
                                        @CurrentUser UserInfo currentUser) {
//        Optional<Lecture> optionalLecture = this.lectureRepository.findById(id);
//        if(optionalLecture.isEmpty()) {
//            return ResponseEntity.notFound().build();
//        }
//        Lecture lecture = optionalLecture.get();
        Lecture lecture = getLectureExistOrElseThrow(id);
        LectureResDto lectureResDto = modelMapper.map(lecture, LectureResDto.class);
        if (lecture.getUserInfo() != null)
            lectureResDto.setEmail(lecture.getUserInfo().getEmail());
        LectureResource lectureResource = new LectureResource(lectureResDto);
        //인증토큰의 email 과 Lecture 가 참조하는 email 주소가 같으면 update 링크를 제공하기
        if ((lecture.getUserInfo() != null) && (lecture.getUserInfo().equals(currentUser))) {
            lectureResource.add(linkTo(LectureController.class)
                    .slash(lecture.getId()).withRel("update-lecture"));
        }
        return ResponseEntity.ok(lectureResource);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> queryLectures(Pageable pageable,
                                           PagedResourcesAssembler<LectureResDto> assembler,
                                           @CurrentUser UserInfo currentUser) {
        Page<Lecture> lecturePage = this.lectureRepository.findAll(pageable);
        //Page<Lecture> => Page<LectureResDto> 매핑
        Page<LectureResDto> lectureResDtoPage =
                lecturePage.map(lecture -> {
                    LectureResDto lectureResDto = new LectureResDto();
                    if (lecture.getUserInfo() != null) {
                        lectureResDto.setEmail(lecture.getUserInfo().getEmail());
                    }
                    modelMapper.map(lecture, lectureResDto);
                    return lectureResDto;
                });
//        PagedModel<EntityModel<LectureResDto>> pagedModel = assembler.toModel(lectureResDtoPage);
        PagedModel<LectureResource> pagedModel =
                //assembler.toModel(lectureResDtoPage, lectureResDto -> new LectureResource(lectureResDto));
                assembler.toModel(lectureResDtoPage, LectureResource::new);
        if (currentUser != null) {
            pagedModel.add(linkTo(LectureController.class).withRel("create-lecture"));
        }
        return ResponseEntity.ok(pagedModel);
    }


    @PostMapping
    public ResponseEntity<?> createLecture(@RequestBody @Valid LectureReqDto lectureReqDto,
                                           Errors errors,
                                           @CurrentUser UserInfo currentUser) {
        //입력항목 오류 체크
        if(errors.hasErrors()) {
            return badRequest(errors);
        }

        //입력항목 biz logic 체크
        this.lectureValidator.validate(lectureReqDto, errors);
        if(errors.hasErrors()) {
            return badRequest(errors);
        }

        //Dto => Entity Convert
        Lecture lecture = modelMapper.map(lectureReqDto, Lecture.class);
        //free, offline 필드 update
        lecture.update();
        lecture.setLectureStatus(LectureStatus.PUBLISHED);

        //Lecture 와 UserInfo 연관관계 설정
        lecture.setUserInfo(currentUser);

        Lecture addLecture = this.lectureRepository.save(lecture);
        //Entity => Dto Convert
        LectureResDto lectureResDto = modelMapper.map(addLecture, LectureResDto.class);
        //LectureResDto 에 UserInfo 객체의 email set
        lectureResDto.setEmail(addLecture.getUserInfo().getEmail());

        WebMvcLinkBuilder selfLinkBuilder =
                WebMvcLinkBuilder.linkTo(LectureController.class).slash(lectureResDto.getId());
        URI createUri = selfLinkBuilder.toUri();

        LectureResource lectureResource = new LectureResource(lectureResDto);
        lectureResource.add(linkTo(LectureController.class).withRel("query-lectures"));
        //lectureResource.add(selfLinkBuilder.withSelfRel());
        lectureResource.add(selfLinkBuilder.withRel("update-lecture"));

        return ResponseEntity.created(createUri).body(lectureResource);
    }

    private static ResponseEntity<?> badRequest(Errors errors) {
        return ResponseEntity.badRequest().body(new ErrorsResource(errors));
    }
}
