package io.gitee.zerowsh.actable.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.gitee.zerowsh.actable.demo.entity.ZeroEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ZeroMapper extends BaseMapper<ZeroEntity> {
    ZeroEntity getInfo();
}
