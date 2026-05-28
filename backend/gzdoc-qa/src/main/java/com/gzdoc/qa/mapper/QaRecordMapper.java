package com.gzdoc.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gzdoc.qa.entity.QaRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答记录Mapper
 */
@Mapper
public interface QaRecordMapper extends BaseMapper<QaRecord> {
}
