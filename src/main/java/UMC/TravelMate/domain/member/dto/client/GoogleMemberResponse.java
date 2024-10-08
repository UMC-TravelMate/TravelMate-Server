package UMC.TravelMate.domain.member.dto.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleMemberResponse {
    private String sub;
    private String name;
}