insert into accounting.accounts (id,name, description, current_balance) values ('25e575be-1123-435a-aea4-d45ec68796f5','foo','bar',42);

insert into accounting.codas (id,filename, content, account_id, sequence_number) VALUES 
('4fc22296-4313-4845-b823-5f97268e26cc', 'test', 'test', '25e575be-1123-435a-aea4-d45ec68796f5', 1);

insert into accounting.movement_categories (id, name, description, type) VALUES ('93b87203-896a-4951-8c69-3f739e5df96e', 'test', 'test', 'test');

insert into accounting.movements (id, amount, entry_date, account_id, coda_id, coda_sequence_number, counter_party_account_number, counter_party_name, communication, category_id) VALUES 
('6e289df4-1e35-4206-9b12-ab1723c6f446', 1, now(), '25e575be-1123-435a-aea4-d45ec68796f5', '4fc22296-4313-4845-b823-5f97268e26cc', 1, 'test', 'test', 'test', '93b87203-896a-4951-8c69-3f739e5df96e');

