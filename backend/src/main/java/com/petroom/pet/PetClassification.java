package com.petroom.pet;

import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;

/**
 * 분류 · 세부 분류 · 관리 방식 조합 규칙.
 * <ul>
 *   <li>포유류: 세부 분류 강아지·고양이, 개체 단위</li>
 *   <li>조류: 세부 분류 핀치류·앵무류, 개체 단위</li>
 *   <li>어류: 세부 분류 없음(종은 사용자가 입력), 개체 단위 또는 어항 단위</li>
 * </ul>
 */
public final class PetClassification {

    private PetClassification() {
    }

    public static void validate(PetCategory category, PetSubCategory subCategory, ManagementType managementType) {
        if (category == PetCategory.FISH) {
            if (subCategory != null) {
                throw new BusinessException(ErrorCode.INVALID_PET_CLASSIFICATION, "어류는 세부 분류 없이 종을 직접 입력합니다.");
            }
            return;
        }
        if (subCategory == null || subCategory.category() != category) {
            throw new BusinessException(ErrorCode.INVALID_PET_CLASSIFICATION, "분류에 맞는 세부 분류를 선택해 주세요.");
        }
        if (managementType != ManagementType.INDIVIDUAL) {
            throw new BusinessException(ErrorCode.INVALID_PET_CLASSIFICATION, "어항 단위 관리는 어류만 가능합니다.");
        }
    }
}
