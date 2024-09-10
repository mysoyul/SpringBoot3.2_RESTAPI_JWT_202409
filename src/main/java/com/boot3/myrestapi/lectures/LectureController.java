package com.boot3.myrestapi.lectures;

import com.boot3.myrestapi.commons.exception.BusinessException;
import com.boot3.myrestapi.commons.resource.ErrorsResource;
import com.boot3.myrestapi.lectures.dto.LectureReqDto;
import com.boot3.myrestapi.lectures.dto.LectureResDto;
import com.boot3.myrestapi.lectures.dto.LectureResource;
import com.boot3.myrestapi.lectures.validator.LectureValidator;

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
                                        Errors errors) {

        Lecture existingLecture = getLectureExistOrElseThrow(id);

        if (errors.hasErrors()) {
            return badRequest(errors);
        }
        lectureValidator.validate(lectureReqDto, errors);
        if (errors.hasErrors()) {
            return badRequest(errors);
        }

        this.modelMapper.map(lectureReqDto, existingLecture);
        existingLecture.update();
        Lecture savedLecture = this.lectureRepository.save(existingLecture);
        LectureResDto lectureResDto = modelMapper.map(savedLecture, LectureResDto.class);

        LectureResource lectureResource = new LectureResource(lectureResDto);
        return ResponseEntity.ok(lectureResource);
    }

    private Lecture getLectureExistOrElseThrow(Integer id) {
        String errMsg = String.format("Id = %d Lecture Not Found", id);
        return this.lectureRepository.findById(id)
                .orElseThrow(() -> new BusinessException(errMsg, HttpStatus.NOT_FOUND));
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getLecture(@PathVariable Integer id) {
//        Optional<Lecture> optionalLecture = this.lectureRepository.findById(id);
//        if(optionalLecture.isEmpty()) {
//            return ResponseEntity.notFound().build();
//        }
//        Lecture lecture = optionalLecture.get();
        Lecture lecture = getLectureExistOrElseThrow(id);
        LectureResDto lectureResDto = modelMapper.map(lecture, LectureResDto.class);
        LectureResource lectureResource = new LectureResource(lectureResDto);
        return ResponseEntity.ok(lectureResource);
    }

    @GetMapping
    public ResponseEntity<?> queryLectures(Pageable pageable,PagedResourcesAssembler<LectureResDto> assembler) {
        Page<Lecture> lecturePage = this.lectureRepository.findAll(pageable);
        //Page<Lecture> => Page<LectureResDto> 매핑
        Page<LectureResDto> lectureResDtoPage =
                lecturePage.map(lecture -> modelMapper.map(lecture, LectureResDto.class));
//        PagedModel<EntityModel<LectureResDto>> pagedModel = assembler.toModel(lectureResDtoPage);
        PagedModel<LectureResource> pagedModel =
                //assembler.toModel(lectureResDtoPage, lectureResDto -> new LectureResource(lectureResDto));
                assembler.toModel(lectureResDtoPage, LectureResource::new);
        return ResponseEntity.ok(pagedModel);
    }


    @PostMapping
    public ResponseEntity<?> createLecture(@RequestBody @Valid LectureReqDto lectureReqDto, Errors errors) {
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

        Lecture addLecture = this.lectureRepository.save(lecture);
        //Entity => Dto Convert
        LectureResDto lectureResDto = modelMapper.map(addLecture, LectureResDto.class);

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
