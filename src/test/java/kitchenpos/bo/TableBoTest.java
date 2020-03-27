package kitchenpos.bo;

import kitchenpos.Fixtures;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.model.OrderStatus;
import kitchenpos.model.OrderTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TableBoTest {
    @Mock
    public OrderDao orderDao;

    @Mock
    public OrderTableDao orderTableDao;

    @InjectMocks
    public TableBo tableBo;

    private OrderTable defaultOrderTable;
    private List<OrderTable> defaultOrderTableList = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        defaultOrderTable = Fixtures.getOrderTable(1L, true, 0);

        defaultOrderTableList.add(defaultOrderTable);
        defaultOrderTableList.add(Fixtures.getOrderTable(2L, false, 2));
        defaultOrderTableList.add(Fixtures.getOrderTable(3L, false, 3));
    }

    @DisplayName("정상적인 값으로 테이블의 정상 생성된다.")
    @Test
    public void createNormal() {
        given(orderTableDao.save(defaultOrderTable)).willReturn(defaultOrderTable);

        OrderTable createdOrderTable = tableBo.create(defaultOrderTable);

        assertThat(createdOrderTable).isNotNull();
    }

    @DisplayName("테이블을 목록을 조회할 수 있다.")
    @Test
    public void list() {
        given(orderTableDao.findAll()).willReturn(defaultOrderTableList);

        List<OrderTable> orderTableList = tableBo.list();

        assertThat(orderTableList).contains(defaultOrderTableList.toArray(new OrderTable[0]));
    }

    @DisplayName("테이블이 요리중이거나 식사 상태라면 테이블을 빈상태로 변경할 수 없다.")
    @Test
    public void changeEmptyOnlyCompletion() {
        given(orderTableDao.findById(1L))
                .willReturn(java.util.Optional.ofNullable(defaultOrderTable));
        given(orderDao.existsByOrderTableIdAndOrderStatusIn(
                1L,
                Arrays.asList(OrderStatus.COOKING.name(), OrderStatus.MEAL.name())))
                .willReturn(true);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(()
                -> tableBo.changeEmpty(1L, defaultOrderTable));
    }

    @DisplayName("테이블이 테이블 그룹에 속해 있을 경우 테이블을 빈상태로 변경할 수 없다.")
    @Test
    public void changeEmptyNotIncludedTableGroup() {
        defaultOrderTable.setTableGroupId(2L);
        given(orderTableDao.findById(1L))
                .willReturn(java.util.Optional.ofNullable(defaultOrderTable));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(()
                -> tableBo.changeEmpty(1L, defaultOrderTable));
    }

    @DisplayName("변경하려는 손님의 수가 0 미만이면 손님 수를 변경 불가능하다.")
    @ParameterizedTest
    @ValueSource(ints = {-1, -2})
    public void changeNumberOfGuestsNegativeGuest(int numberOfGuest) {
        defaultOrderTable.setNumberOfGuests(numberOfGuest);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> tableBo.changeNumberOfGuests(1L, defaultOrderTable));
    }


    @DisplayName("변경하려는 손님의 수가 0 이상이면 손님의 수를 변경 가능하다.")
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void changeNumberOfGuestsOnlyPositiveGuest(int numberOfGuest) {
        defaultOrderTable.setNumberOfGuests(numberOfGuest);
        OrderTable savedOrderTable = Fixtures.getOrderTable(1L, false,2);
        given(orderTableDao.findById(anyLong())).willReturn(java.util.Optional.of(savedOrderTable));
        given(orderTableDao.save(savedOrderTable)).willReturn(defaultOrderTable);

        OrderTable changedOrderTable = tableBo.changeNumberOfGuests(savedOrderTable.getId(), defaultOrderTable);

        assertThat(changedOrderTable.getNumberOfGuests()).isEqualTo(defaultOrderTable.getNumberOfGuests());
    }

    @DisplayName("테이블이 비어 있을 경우 손님의 수 변경이 불가하다.")
    @Test
    public void changeNumberNotEmptyTable() {
        OrderTable orderTable = Fixtures.getOrderTable(1L, false, 3);
        given(orderTableDao.findById(1L)).willReturn(java.util.Optional.of(defaultOrderTable));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> tableBo.changeNumberOfGuests(1L, orderTable)
        );
    }

}