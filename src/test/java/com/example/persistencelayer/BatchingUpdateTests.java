package com.example.persistencelayer;

import com.example.persistencelayer.model.Customer;
import com.example.persistencelayer.repository.CustomerRepository;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class BatchingUpdateTests {

    @PersistenceContext
    private EntityManager entityManager;


    @Autowired
    CustomerRepository customerRepository;

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 20, 30, 50, 60, 80, 100})
    @Transactional
    void separateSessionsUpdate(int batchSize) {
        long sum = 0;

        entityManager.unwrap(Session.class).setJdbcBatchSize(batchSize);

        List<Customer> customerList = customerRepository.findBySurnameContaining("surname5");
        customerList.forEach(x -> x.setSurname("TESTSURNAME"));
        List[] lists = split(customerList, batchSize);


        while (!lists[1].isEmpty()) {

            long start = System.currentTimeMillis();
            TestUtils t = new TestUtils();
            t.updateTransaction(lists[0], customerRepository);

            long end = System.currentTimeMillis();
            sum += end - start;
            customerList = lists[1];


            lists = split(customerList, batchSize);
        }
        System.out.println(batchSize + ": " + sum);

    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 20, 30, 50, 60, 80, 100})
    @Transactional
    void singleSessionUpdate(int batchSize) {
        long sum = 0;

        entityManager.unwrap(Session.class).setJdbcBatchSize(batchSize);

        List<Customer> customerList = customerRepository.findBySurnameContaining("surname5");
        customerList.forEach(x -> x.setSurname("TESTSURNAME"));


        long start = System.currentTimeMillis();
        TestUtils t = new TestUtils();
        t.updateTransaction(customerList, customerRepository);

        long end = System.currentTimeMillis();
        System.out.println(batchSize + ": " + (end - start));

    }


    public static List[] split(List<Customer> list, int size) {

        // Creating two empty lists
        List<Customer> first = new ArrayList<>();
        List<Customer> second = new ArrayList<>();

        if (size >= list.size()) {
            return new List[]{list, second};
        }

        for (int i = 0; i < size; i++)
            first.add(list.get(i));


        for (int i = size; i < list.size(); i++)
            second.add(list.get(i));

        // Returning a List of array
        return new List[]{first, second};
    }


}
