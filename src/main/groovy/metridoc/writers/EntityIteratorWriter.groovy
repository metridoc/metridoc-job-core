package metridoc.writers

import metridoc.entities.MetridocRecordEntity
import metridoc.iterators.RowIterator
import org.hibernate.SessionFactory

/**
 * Created with IntelliJ IDEA on 7/2/13
 * @author Tommy Barker
 */
class EntityIteratorWriter extends DefaultIteratorWriter {

    SessionFactory sessionFactory
    Class<MetridocRecordEntity> recordEntityClass

    @Override
    WriteResponseTotals write(RowIterator rowIterator) {
        def session = sessionFactory.currentSession
        def transaction = session.beginTransaction()

        try {
            def response = super.write(rowIterator)
            transaction.commit()
            return response
        }
        catch (Exception e) {
            transaction.rollback()
            throw e
        }
        finally {
            if (session.isOpen()) {
                session.close()
            }
        }
    }

    @Override
    boolean doWrite(int lineNumber, Map<String, Object> record) {
        def instance = recordEntityClass.newInstance()
        if (instance.acceptRecord(record)) {
            instance.populate(record)
            if (instance.shouldSave()) {
                sessionFactory.currentSession.save(instance)
                return true
            }
        }

        return false
    }
}
