package io.gitee.zerowsh.actable.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.gitee.zerowsh.actable.demo.service.IZeroService;
import org.springframework.stereotype.Service;
import io.gitee.zerowsh.actable.demo.entity.ZeroEntity;
import io.gitee.zerowsh.actable.demo.mapper.ZeroMapper;

@Service
public class ZeroServiceImpl extends ServiceImpl<ZeroMapper, ZeroEntity> implements IZeroService {
}
