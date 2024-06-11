package clash.manage.dao;

import clash.manage.model.CdnSpeed;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CdnSpeedMapper {

    int insert(CdnSpeed record);

    int update(CdnSpeed record);

    CdnSpeed selectByIp(@Param("ip") String ip);

    List<CdnSpeed> listGoodIp(@Param("cdnName") String cdnName, @Param("limitNum") int limitNum);

    List<CdnSpeed> listBestIp(@Param("cdnName") String cdnName);
}