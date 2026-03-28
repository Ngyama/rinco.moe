package com.rinco.vnanalytics.api.staff.service;

import com.rinco.vnanalytics.api.staff.mapper.StaffMapper;
import com.rinco.vnanalytics.api.staff.mapper.StaffSchemaMapper;
import com.rinco.vnanalytics.api.staff.model.StaffPersonItem;
import com.rinco.vnanalytics.api.staff.model.StaffPersonsResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StaffService {

    private final StaffMapper staffMapper;

    public StaffService(StaffMapper staffMapper, StaffSchemaMapper staffSchemaMapper) {
        this.staffMapper = staffMapper;
        staffSchemaMapper.ensureAll();
    }

    public StaffPersonsResponse fetchPersons() {
        List<Object[]> rows = staffMapper.queryAllPersons();
        List<StaffPersonItem> persons = new ArrayList<>();
        for (Object[] r : rows) {
            persons.add(new StaffPersonItem(
                    (Long) r[0],
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    (String) r[4],
                    (String) r[5]
            ));
        }
        return new StaffPersonsResponse(persons);
    }
}
