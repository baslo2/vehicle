package home.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import home.IConsts;
import home.Storage;
import home.models.AbstractVehicle;
import home.models.Car;
import home.models.Motorcycle;
import home.models.Truck;
import home.models.VehicleType;
import home.utils.LogUtils;

abstract sealed class AbstractDao implements IDao permits DaoSQLite {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDao.class);

    private static final String SELECT_ALL = "SELECT * FROM vehicle;";

    private static final String SELECT_ONE = "SELECT * FROM vehicle WHERE id=?;";

    private static final String INSERT = """
            INSERT INTO vehicle
            ('type','color','number','date_time','is_transports_cargo',
            'is_transports_passengers','has_trailer','has_cradle')
            VALUES (?,?,?,?,?,?,?,?);""";

    private static final String UPDATE = """
            UPDATE vehicle SET 
            type = ?, color = ?, number = ?, date_time = ?,
            is_transports_cargo = ?, is_transports_passengers = ?,
            has_trailer = ?, has_cradle = ? WHERE id = ?;""";

    private static final String DELETE = "DELETE FROM vehicle WHERE id IN (%s);";

    private static final String CONNECTION_ERROR_CODE = "08";

    protected AbstractDao() {
    }

    protected abstract Connection getConnection() throws SQLException;

    protected abstract int getTransactionIsolation();

    protected abstract Logger getLogger();

    @Override
    public AbstractVehicle readOne(long id) throws SQLException {
        try (var conn = getConnection()) {
            conn.setTransactionIsolation(getTransactionIsolation());

            try (var pstmt = conn.prepareStatement(SELECT_ONE)) {
                pstmt.setLong(1, id);

                var dataObjs = new ArrayList<AbstractVehicle>();
                try (var res = pstmt.executeQuery()) {
                    while (res.next()) {
                        dataObjs.add(convertResultToDataObj(res));
                    }
                }

                if (dataObjs.size() > 1) {
                    throw new SQLException("more then one object with this id: " + id);
                }

                return dataObjs.get(0);
            }
        }
    }

    @Override
    public List<AbstractVehicle> readAll() throws SQLException {
        try (var conn = getConnection()) {
            conn.setTransactionIsolation(getTransactionIsolation());
            try (var stmt = conn.createStatement();
                 var res = stmt.executeQuery(SELECT_ALL)) {
                var dataObjs = new ArrayList<AbstractVehicle>();
                while (res.next()) {
                    dataObjs.add(convertResultToDataObj(res));
                }
                return dataObjs;
            }
        }
    }

    private AbstractVehicle convertResultToDataObj(ResultSet res) throws SQLException {
        var type = res.getString(IDbConsts.TYPE);
        var vehicleType = VehicleType.getVehicleType(type);
        if (vehicleType == null) {
            throw new SQLException("Wrong type: " + type);
        }

        AbstractVehicle vehicle = switch (vehicleType) {
            case CAR -> {
                var car = new Car();
                car.setTransportsPassengers(convertToBoolean(res.getInt(IDbConsts.IS_TRANSPORTS_PASSENGERS)));
                car.setHasTrailer(convertToBoolean(res.getInt(IDbConsts.HAS_TRAILER)));
                yield car;
            }
            case TRUCK -> {
                var truck = new Truck();
                truck.setTransportsCargo(convertToBoolean(res.getInt(IDbConsts.IS_TRANSPORTS_CARGO)));
                truck.setHasTrailer(convertToBoolean(res.getInt(IDbConsts.HAS_TRAILER)));
                yield truck;
            }
            case MOTORCYCLE -> {
                var motorcycle = new Motorcycle();
                motorcycle.setHasCradle(convertToBoolean(res.getInt(IDbConsts.HAS_CRADLE)));
                yield motorcycle;
            }
        };

        vehicle.setId(res.getLong(IDbConsts.ID));
        vehicle.setColor(res.getString(IDbConsts.COLOR));
        vehicle.setNumber(res.getString(IDbConsts.NUMBER));
        vehicle.setDateTime(res.getLong(IDbConsts.DATE_TIME));

        return vehicle;
    }

    private boolean convertToBoolean(int intBoolean) throws SQLException {
        return switch (intBoolean) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new SQLException("Wrong logic value: " + intBoolean);
        };
    }

    @Override
    public void saveAllChanges() throws SQLException {
        var exceptions = new ArrayList<SQLException>();

        try {
            Long[] idsForDel = Storage.INSTANCE.getIdsForDel();
            if (idsForDel.length > 0) {
                delete(idsForDel);
            }
        } catch (IllegalStateException e) {
            exceptions.add(new SQLException("Delete operation error.", e));
        }

        try {
            Set<Long> idsForUpdate = Storage.INSTANCE.getIdsForUpdate();
            operation(this::update, dataObj -> dataObj.getId() > 0 && idsForUpdate.contains(dataObj.getId()));
        } catch (IllegalStateException e) {
            exceptions.add(new SQLException("Update operation error.", e));
        }

        try {
            operation(this::insert, dataObj -> dataObj.getId() == 0);
        } catch (IllegalStateException e) {
            exceptions.add(new SQLException("Insert operation error.", e));
        }

        if (!exceptions.isEmpty()) {
            var mainExceptions = new SQLException("Save all changes operation error.");
            exceptions.forEach(mainExceptions::addSuppressed);
            throw mainExceptions;
        }
    }

    private void operation(Consumer<List<AbstractVehicle>> sqlOperation,
            Predicate<AbstractVehicle> objFilter) {
        List<AbstractVehicle> objs = Storage.INSTANCE.getAll().stream()
                .filter(objFilter).collect(Collectors.toList());
        if (!objs.isEmpty()) {
            sqlOperation.accept(objs);
        }
    }

    @Override
    public void saveAs() throws SQLException {
        try {
            insert(Storage.INSTANCE.getAll());
        } catch (IllegalStateException e) {
            throw new SQLException("Save as operation error (insert).", e);
        }
    }

    private void insert(List<AbstractVehicle> dataObjs) {
        sqlOperationBatch(false, dataObjs, "The information has not been added to the database: %s");
    }

    public void update(List<AbstractVehicle> dataObjs) {
        sqlOperationBatch(true, dataObjs, "Thw information in the database has not been updated: %s");
    }

    private void sqlOperationBatch(boolean isUpdateOperation, List<AbstractVehicle> dataObjs, String errorMsg) {
        String sql = isUpdateOperation ? UPDATE : INSERT;

        try (var conn = getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(getTransactionIsolation());
            try (var pstmt = conn.prepareStatement(sql)) {
                int operationsCount = 0;
                for (AbstractVehicle dataObj : dataObjs) {
                    pstmt.clearParameters();
                    fillStmtByDataFromObj(pstmt, dataObj, isUpdateOperation);
                    pstmt.addBatch();
                    operationsCount++;

                    // execute each 1k items
                    if (operationsCount % 1_000 == 0 || operationsCount == dataObjs.size()) {
                        checkBatchExecution(pstmt.executeBatch(),
                                String.format(errorMsg, dataObj), getLogger());
                        conn.commit();
                    }
                }
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                String error = String.format(errorMsg, IConsts.EMPTY_STRING);

                checkConnectionState(e, errorMsg);

                rollBackAndLog(conn, e, error);
                sqlOperationOneByOne(conn, sql, dataObjs, isUpdateOperation, error);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL insert/update operation errror: ", e);
        }
    }

    void checkBatchExecution(int[] batchResults, String errorMsg, Logger log) throws SQLException {
        if (batchResults == null) {
            log.warn("Batch execution result is null.\nCheck!\nMaybe " + errorMsg);
            return;
        }

        for (int batchResalt : batchResults) {
            if (batchResalt >= 0 || Statement.SUCCESS_NO_INFO == batchResalt) {
                // everything is fine.
                continue;
            }

            var msg = new StringBuilder();
            msg.append("Batch execution error:\n").append(errorMsg)
                    .append("\n").append("When executing the batch, ");

            if (Statement.EXECUTE_FAILED == batchResalt) {
                msg.append("result code 'EXECUTE_FAILED' was received.");
                throw LogUtils.logAndCreateSqlException(msg.toString(), log);
            }

            msg.append("unknown result code '").append(batchResalt).append("' was received.");
            throw LogUtils.logAndCreateSqlException(msg.toString(), log);
        }
    }

    private void rollBackAndLog(Connection conn, Exception e, String errorMsg) {
        LOG.error(errorMsg, e);
        try {
            conn.rollback();
        } catch (SQLException ex) {
            throw LogUtils.logAndCreateIllegalStateException(errorMsg + " SQL rollback error.", LOG, e);
        }
    }

    private void checkConnectionState(SQLException e, String errorMsg) throws SQLException {
        String sqlState = e.getSQLState();
        if (sqlState.startsWith(CONNECTION_ERROR_CODE)) {
            throw LogUtils.logAndCreateSqlException("%s:\nConnection error (code %s)".formatted(errorMsg, sqlState),
                    getLogger());
        }
    }

    private void sqlOperationOneByOne(Connection conn, String sql, List<AbstractVehicle> dataObjs,
            boolean isUpdateOperation, String errorMsg) throws SQLException {
        String operationType = isUpdateOperation ? "update" : "insert";

        Exception mainException = null;
        var errorsWithDataObjs = new ArrayList<String>();

        conn.setAutoCommit(true);
        for (AbstractVehicle dataObj : dataObjs) {
            try (var pstmt = conn.prepareStatement(sql)) {
                fillStmtByDataFromObj(pstmt, dataObj, isUpdateOperation);
                pstmt.execute();
            } catch (SQLException e) {
                mainException = addException(mainException, e,
                        "Exception in %s mechanism one by one.".formatted(operationType));
                errorsWithDataObjs.add(dataObj.toString() + "\n\t(" + e.getMessage() + ')');
            }
        }

        if (!errorsWithDataObjs.isEmpty()) {
            var sb = new StringBuilder();
            sb.append(errorMsg).append(" Can't ").append(operationType).append(": \n")
                    .append(String.join("\n", errorsWithDataObjs));

            throw LogUtils.logAndCreateSqlException(sb.toString(), LOG, mainException);
        }
    }

    private void fillStmtByDataFromObj(PreparedStatement pstmt, AbstractVehicle dataObj,
            boolean isUpdateOperation) throws SQLException {
        VehicleType dataObjType = dataObj.getType();

        pstmt.setString(1, dataObjType.getType());
        pstmt.setString(2, dataObj.getColor());
        pstmt.setString(3, dataObj.getNumber());
        pstmt.setLong(4, dataObj.getDateTime());

        switch (dataObjType) {
            case CAR:
                Car car = (Car) dataObj;
                pstmt.setInt(6, converToInt(car.isTransportsPassengers()));
                pstmt.setInt(7, converToInt(car.hasTrailer()));
                break;

            case TRUCK:
                Truck truck = (Truck) dataObj;
                pstmt.setInt(5, converToInt(truck.isTransportsCargo()));
                pstmt.setInt(7, converToInt(truck.hasTrailer()));
                break;

            case MOTORCYCLE:
                pstmt.setInt(8, converToInt(((Motorcycle) dataObj).hasCradle()));
                break;
        }

        if (isUpdateOperation) {
            pstmt.setLong(9, dataObj.getId());
        }
    }

    private int converToInt(boolean booleanVal) {
        return booleanVal ? 1 : 0;
    }

    private void delete(Long[] ids) {
        String idsStr = Stream.of(ids).map(String::valueOf).collect(Collectors.joining(","));
        String sql = String.format(DELETE, idsStr);
        try (var conn = getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(getTransactionIsolation());
            try (var pstmt = conn.prepareStatement(sql)) {
                if (pstmt.executeUpdate() <= 0) {
                    throw new SQLException("Information has not been deleted from the database."
                            + "\nid list for delete: " + idsStr);
                }
                conn.commit();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                String errorMsg = "Error while removal several rose in one query.";

                checkConnectionState(e, errorMsg);

                rollBackAndLog(conn, e, errorMsg);
                deleteOneByOne(conn, ids);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL delete operation error: ", e);
        }
    }

    private void deleteOneByOne(Connection conn, Long[] ids) throws SQLException {
        Exception mainException = null;
        var errorsWithIds = new ArrayList<String>();

        conn.setAutoCommit(true);
        String sql = String.format(DELETE, "?");

        for (Long id : ids) {
            try (var pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, id);
                if (pstmt.executeUpdate() <= 0) {
                    errorsWithIds.add(id + IConsts.EMPTY_STRING);
                }
            } catch (SQLException e) {
                mainException = addException(mainException, e, "Exception in delete mechanism one by one");
                errorsWithIds.add(id + "\n\t(" + e.getMessage() + ')');
            }
        }

        if (!errorsWithIds.isEmpty()) {
            var sb = new StringBuilder();
            sb.append("Information has not been deleted from the database")
                    .append("\nCan't delete objects with ids:\n")
                    .append(String.join("\n", errorsWithIds));

            throw LogUtils.logAndCreateSqlException(sb.toString(), LOG, mainException);
        }
    }

    private Exception addException(Exception mainException, Exception newException, String msg) {
        if (mainException == null) {
            mainException = new SQLException(msg);
        }
        mainException.addSuppressed(newException);
        return mainException;
    }
}