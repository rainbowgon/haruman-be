package ssafy.haruman.domain.challenge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ssafy.haruman.domain.challenge.dto.response.AccumulatedAmountResponseDto;
import ssafy.haruman.domain.challenge.dto.response.ChallengeHistoryResponseDto;
import ssafy.haruman.domain.challenge.dto.response.ChallengeUserInfoDto;
import ssafy.haruman.domain.challenge.dto.response.ChallengeUserListResponseDto;
import ssafy.haruman.domain.challenge.entity.Challenge;
import ssafy.haruman.domain.challenge.entity.ChallengeGroup;
import ssafy.haruman.domain.challenge.repository.ChallengeRepository;
import ssafy.haruman.domain.challenge.repository.ChallengeUserInfoMapping;
import ssafy.haruman.domain.profile.entity.Profile;
import ssafy.haruman.global.service.S3FileService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final ChallengeRepository challengeRepository;
    private final S3FileService s3FileService;


    @Override
    public List<ChallengeUserListResponseDto> selectChallengeUserList() {

        List<ChallengeUserInfoMapping> challengeList = challengeRepository.findChallengeAndExpenseAndProfileByStatus();

        return challengeList.stream()
                .collect(Collectors.groupingBy(this::getGroupKey))
                .entrySet().stream()
                .map(entry -> ChallengeUserListResponseDto.from(entry.getKey(), convertToUserInfoDto(entry.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public AccumulatedAmountResponseDto selectAccumulatedAmount(Profile profile) {

        Integer accumulatedAmount = challengeRepository.sumByProfileAndStatus(profile.getId());

        return AccumulatedAmountResponseDto.builder().accumulatedAmount(accumulatedAmount).build();
    }

    @Override
    public List<ChallengeHistoryResponseDto> selectChallengeHistory(Profile profile, Date yearAndMonth) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
        String date = yearAndMonth == null ?
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) : dateFormat.format(yearAndMonth);

        List<Challenge> challengeList = challengeRepository.findAllByProfileAndDate(profile.getId(), date);

        return challengeList.stream()
                .map(ChallengeHistoryResponseDto::from)
                .collect(Collectors.toList());
    }

    private String getGroupKey(ChallengeUserInfoMapping challenge) {
        ChallengeGroup group = ChallengeGroup.getGroup(challenge.getUsedAmount());
        return group.getGroupKey();
    }

    private List<ChallengeUserInfoDto> convertToUserInfoDto(List<ChallengeUserInfoMapping> list) {
        return list.stream()
                .map(item -> {

                    String profileImageUrl = s3FileService.getS3Url(item.getProfileImagePath(), item.getProfileImageName());
                    return ChallengeUserInfoDto.from(item, profileImageUrl);
                })
                .collect(Collectors.toList());
    }
}
