package com.dormrepair.file;
import com.dormrepair.common.enums.*; import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.*;
class FileUploadContractTest {
 @Test void parsesBusinessTypesAndRestrictsRoles(){assertThat(FileBizTypeEnum.fromCode("repair")).isEqualTo(FileBizTypeEnum.REPAIR);assertThat(FileBizTypeEnum.REPAIR.allows(UserRoleEnum.STUDENT)).isTrue();assertThat(FileBizTypeEnum.REWORK.allows(UserRoleEnum.STUDENT)).isTrue();assertThat(FileBizTypeEnum.PROCESS.allows(UserRoleEnum.WORKER)).isTrue();assertThat(FileBizTypeEnum.PROCESS.allows(UserRoleEnum.STUDENT)).isFalse();assertThat(FileBizTypeEnum.REPAIR.allows(UserRoleEnum.ADMIN)).isFalse();}
 @Test void fileErrorsUseExpectedHttpFamilies(){assertThat(ResultCodeEnum.FILE_NOT_FOUND.getCode()).isEqualTo(18005);assertThat(ResultCodeEnum.FILE_ALREADY_BOUND.getCode()).isEqualTo(18007);}
}
