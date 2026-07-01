BEGIN;

DELETE FROM support_ticket_messages
WHERE ticket_id IN (
    '90000000-0000-0000-0000-000000000001'
);

DELETE FROM support_tickets
WHERE id IN (
    '90000000-0000-0000-0000-000000000001'
);

DELETE FROM messages
WHERE conversation_id IN (
    '80000000-0000-0000-0000-000000000001'
);

DELETE FROM conversations
WHERE id IN (
    '80000000-0000-0000-0000-000000000001'
);

DELETE FROM offers
WHERE id IN (
    '70000000-0000-0000-0000-000000000001'
);

DELETE FROM loads
WHERE id IN (
    '60000000-0000-0000-0000-000000000001',
    '60000000-0000-0000-0000-000000000002'
);

DELETE FROM company_subscriptions
WHERE company_id IN (
    '10000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002'
);

DELETE FROM market_surveys
WHERE company_id IN (
    '10000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000004'
);

DELETE FROM users
WHERE email IN (
    'admin@easyfleetmatch.dev',
    'broker.owner@easyfleetmatch.dev',
    'fleet.owner@easyfleetmatch.dev',
    'review.broker@easyfleetmatch.dev',
    'review.fleet@easyfleetmatch.dev'
);

DELETE FROM companies
WHERE id IN (
    '10000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000004'
);

INSERT INTO companies (
    id,
    legal_name,
    dba_name,
    entity_type,
    ein,
    state_of_formation,
    headquarters,
    normalized_headquarters,
    headquarters_address_verified,
    headquarters_address_verification_status,
    headquarters_latitude,
    headquarters_longitude,
    email,
    phone,
    primary_contact,
    website,
    type,
    fleet_size,
    description,
    verification_status,
    mc_number,
    dot_number,
    authority_status,
    broker_bond_or_trust,
    insurance_coverage,
    operating_regions,
    verification_notes,
    company_information_completed,
    market_survey_completed,
    manual_priority,
    admin_internal_notes,
    created_at,
    updated_at
)
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'Atlas Freight Brokerage LLC',
        'Atlas Freight',
        'LLC',
        '12-3456789',
        'TX',
        '500 W 2nd St, Austin, TX 78701',
        '500 W 2ND ST AUSTIN TX 78701',
        TRUE,
        'VERIFIED',
        30.2672,
        -97.7431,
        'ops@atlasfreight.dev',
        '+15125550100',
        'Aylin Carter',
        'https://atlasfreight.dev',
        'BROKER',
        NULL,
        'Regional broker focused on Texas, Oklahoma, and Louisiana freight.',
        'APPROVED',
        'MC100001',
        'DOT100001',
        'ACTIVE',
        'BMC-84 Active',
        'Cargo 250k / Liability 1M',
        'TX,OK,LA',
        'Seeded approved DEV broker.',
        TRUE,
        TRUE,
        10,
        'High priority launch broker.',
        NOW(),
        NOW()
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'Blue Ridge Fleet Services Inc',
        'Blue Ridge Fleet',
        'Corporation',
        '98-7654321',
        'NC',
        '201 S Tryon St, Charlotte, NC 28202',
        '201 S TRYON ST CHARLOTTE NC 28202',
        TRUE,
        'VERIFIED',
        35.2271,
        -80.8431,
        'dispatch@blueridgefleet.dev',
        '+17045550100',
        'Mert Wilson',
        'https://blueridgefleet.dev',
        'FLEET',
        24,
        'Expedited fleet running box trucks and sprinter vans across the Southeast.',
        'APPROVED',
        'MC200002',
        'DOT200002',
        'ACTIVE',
        NULL,
        'Cargo 150k / Liability 1M',
        'NC,SC,GA,TN,VA',
        'Seeded approved DEV fleet.',
        TRUE,
        TRUE,
        8,
        'Good match for Southeast broker demand.',
        NOW(),
        NOW()
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'Lone Star Cold Chain LLC',
        'Lone Star Cold Chain',
        'LLC',
        '22-1111111',
        'TX',
        '1000 Main St, Houston, TX 77002',
        '1000 MAIN ST HOUSTON TX 77002',
        TRUE,
        'VERIFIED',
        29.7604,
        -95.3698,
        'review@lonestarcoldchain.dev',
        '+17135550100',
        'Deniz Morgan',
        NULL,
        'BROKER',
        NULL,
        'Broker waiting for operations review.',
        'UNDER_REVIEW',
        'MC300003',
        'DOT300003',
        'ACTIVE',
        'BMC-84 Pending confirmation',
        'Cargo 250k / Liability 1M',
        'TX,NM,AZ',
        'Needs bond verification before approval.',
        TRUE,
        TRUE,
        3,
        'Review bond document before approving.',
        NOW(),
        NOW()
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'Pacific Sprinter Group LLC',
        'Pacific Sprinter',
        'LLC',
        '33-2222222',
        'CA',
        '1 Market St, San Francisco, CA 94105',
        '1 MARKET ST SAN FRANCISCO CA 94105',
        TRUE,
        'VERIFIED',
        37.7749,
        -122.4194,
        'review@pacificsprinter.dev',
        '+14155550100',
        'Selin Reed',
        NULL,
        'FLEET',
        7,
        'Small sprinter fleet waiting for operations review.',
        'UNDER_REVIEW',
        'MC400004',
        'DOT400004',
        'ACTIVE',
        NULL,
        'Cargo 100k / Liability 1M',
        'CA,NV,OR',
        'Insurance certificate needs manual review.',
        TRUE,
        TRUE,
        4,
        'Potential Wave 1 West Coast fleet.',
        NOW(),
        NOW()
    );

INSERT INTO users (
    id,
    first_name,
    last_name,
    email,
    phone,
    password,
    platform_role,
    status,
    company_user_role,
    company_id,
    email_verified,
    email_verified_at,
    phone_verified,
    phone_verified_at,
    failed_login_attempts,
    credentials_changed_at,
    created_at,
    updated_at
)
VALUES
    (
        '20000000-0000-0000-0000-000000000001',
        'Dev',
        'Admin',
        'admin@easyfleetmatch.dev',
        '+15550000001',
        '$2y$10$AtspgxG2.PC0ENtX1Diq/eB.EGf26Jnyl3XnWWW9JgtTZGqh72iG2',
        'ADMIN',
        'ACTIVE',
        'OWNER',
        NULL,
        TRUE,
        NOW(),
        TRUE,
        NOW(),
        0,
        NOW(),
        NOW(),
        NOW()
    ),
    (
        '20000000-0000-0000-0000-000000000002',
        'Aylin',
        'Carter',
        'broker.owner@easyfleetmatch.dev',
        '+15550000002',
        '$2y$10$AtspgxG2.PC0ENtX1Diq/eB.EGf26Jnyl3XnWWW9JgtTZGqh72iG2',
        'USER',
        'ACTIVE',
        'OWNER',
        '10000000-0000-0000-0000-000000000001',
        TRUE,
        NOW(),
        TRUE,
        NOW(),
        0,
        NOW(),
        NOW(),
        NOW()
    ),
    (
        '20000000-0000-0000-0000-000000000003',
        'Mert',
        'Wilson',
        'fleet.owner@easyfleetmatch.dev',
        '+15550000003',
        '$2y$10$AtspgxG2.PC0ENtX1Diq/eB.EGf26Jnyl3XnWWW9JgtTZGqh72iG2',
        'USER',
        'ACTIVE',
        'OWNER',
        '10000000-0000-0000-0000-000000000002',
        TRUE,
        NOW(),
        TRUE,
        NOW(),
        0,
        NOW(),
        NOW(),
        NOW()
    ),
    (
        '20000000-0000-0000-0000-000000000004',
        'Deniz',
        'Morgan',
        'review.broker@easyfleetmatch.dev',
        '+15550000004',
        '$2y$10$AtspgxG2.PC0ENtX1Diq/eB.EGf26Jnyl3XnWWW9JgtTZGqh72iG2',
        'USER',
        'IN_REVIEW',
        'OWNER',
        '10000000-0000-0000-0000-000000000003',
        TRUE,
        NOW(),
        TRUE,
        NOW(),
        0,
        NOW(),
        NOW(),
        NOW()
    ),
    (
        '20000000-0000-0000-0000-000000000005',
        'Selin',
        'Reed',
        'review.fleet@easyfleetmatch.dev',
        '+15550000005',
        '$2y$10$AtspgxG2.PC0ENtX1Diq/eB.EGf26Jnyl3XnWWW9JgtTZGqh72iG2',
        'USER',
        'IN_REVIEW',
        'OWNER',
        '10000000-0000-0000-0000-000000000004',
        TRUE,
        NOW(),
        TRUE,
        NOW(),
        0,
        NOW(),
        NOW(),
        NOW()
    );

INSERT INTO market_surveys (
    id,
    company_id,
    company_type,
    operating_states,
    equipment_types,
    average_loads_per_week,
    fleet_size,
    current_load_board,
    current_tms,
    future_integration_interest,
    biggest_operational_challenges,
    created_at,
    updated_at
)
VALUES
    (
        '30000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        'BROKER',
        '["TX", "OK", "LA"]'::jsonb,
        '["BOX_TRUCK_26FT", "SPRINTER_VAN"]'::jsonb,
        65,
        NULL,
        'DAT',
        'Turvo',
        TRUE,
        'Finding verified fleets with fast response times.',
        NOW(),
        NOW()
    ),
    (
        '30000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000002',
        'FLEET',
        '["NC", "SC", "GA", "TN", "VA"]'::jsonb,
        '["BOX_TRUCK_24FT", "BOX_TRUCK_26FT", "SPRINTER_VAN"]'::jsonb,
        45,
        24,
        'Truckstop',
        NULL,
        TRUE,
        'Reducing empty miles and improving broker quality.',
        NOW(),
        NOW()
    ),
    (
        '30000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000003',
        'BROKER',
        '["TX", "NM", "AZ"]'::jsonb,
        '["BOX_TRUCK_26FT"]'::jsonb,
        30,
        NULL,
        'DAT',
        'None',
        TRUE,
        'Cold chain capacity in secondary markets.',
        NOW(),
        NOW()
    ),
    (
        '30000000-0000-0000-0000-000000000004',
        '10000000-0000-0000-0000-000000000004',
        'FLEET',
        '["CA", "NV", "OR"]'::jsonb,
        '["SPRINTER_VAN", "CARGO_VAN"]'::jsonb,
        18,
        7,
        'None',
        NULL,
        FALSE,
        'Consistent regional freight without race-to-bottom pricing.',
        NOW(),
        NOW()
    );

INSERT INTO company_subscriptions (
    id,
    created_at,
    updated_at,
    company_id,
    subscription_plan_id,
    start_date,
    end_date,
    active,
    auto_renew,
    payment_status,
    payment_provider
)
VALUES
    (
        '40000000-0000-0000-0000-000000000001',
        NOW(),
        NOW(),
        '10000000-0000-0000-0000-000000000001',
        (SELECT id FROM subscription_plans WHERE name = 'PRO'),
        CURRENT_DATE,
        CURRENT_DATE + 30,
        TRUE,
        FALSE,
        'ACTIVE',
        'dev-seed'
    ),
    (
        '40000000-0000-0000-0000-000000000002',
        NOW(),
        NOW(),
        '10000000-0000-0000-0000-000000000002',
        (SELECT id FROM subscription_plans WHERE name = 'PRO'),
        CURRENT_DATE,
        CURRENT_DATE + 30,
        TRUE,
        FALSE,
        'ACTIVE',
        'dev-seed'
    );

INSERT INTO loads (
    id,
    broker_company_id,
    created_by_user_id,
    pickup_city,
    pickup_state,
    pickup_date,
    delivery_city,
    delivery_state,
    delivery_date,
    equipment_type,
    weight,
    weight_lbs,
    rate,
    miles,
    commodity,
    reference_number,
    status,
    notes,
    description,
    pickup_street_address,
    pickup_zip_code,
    pickup_location_name,
    pickup_contact_name,
    pickup_contact_phone,
    pickup_time_window_start,
    pickup_time_window_end,
    pickup_instructions,
    delivery_street_address,
    delivery_zip_code,
    delivery_location_name,
    delivery_contact_name,
    delivery_contact_phone,
    delivery_time_window_start,
    delivery_time_window_end,
    delivery_instructions,
    pallet_count,
    piece_count,
    length_inches,
    width_inches,
    height_inches,
    liftgate_required,
    pallet_jack_required,
    dock_high_required,
    residential_delivery,
    created_at,
    updated_at
)
VALUES
    (
        '60000000-0000-0000-0000-000000000001',
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000002',
        'Austin',
        'TX',
        CURRENT_DATE + 2,
        'Atlanta',
        'GA',
        CURRENT_DATE + 4,
        'BOX_TRUCK_26FT',
        8200,
        8200,
        2450.00,
        960,
        'Retail fixtures',
        'ATLAS-DEV-1001',
        'POSTED',
        'Dock high pickup, appointment required.',
        '26ft box truck load from Austin to Atlanta.',
        '500 W 2nd St',
        '78701',
        'Atlas Austin Warehouse',
        'Aylin Carter',
        '+15125550100',
        '08:00',
        '11:00',
        'Check in at west gate.',
        '250 Ted Turner Dr NW',
        '30303',
        'Atlanta Distribution Center',
        'Receiving Desk',
        '+14045550100',
        '09:00',
        '13:00',
        'Call 30 minutes before arrival.',
        10,
        10,
        48,
        40,
        60,
        FALSE,
        FALSE,
        TRUE,
        FALSE,
        NOW(),
        NOW()
    ),
    (
        '60000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000002',
        'Dallas',
        'TX',
        CURRENT_DATE + 1,
        'Tulsa',
        'OK',
        CURRENT_DATE + 2,
        'SPRINTER_VAN',
        1800,
        1800,
        875.00,
        260,
        'Medical supplies',
        'ATLAS-DEV-1002',
        'POSTED',
        'Temperature stable, no hazmat.',
        'Sprinter van load from Dallas to Tulsa.',
        '901 Main St',
        '75202',
        'Dallas Crossdock',
        'Operations Desk',
        '+12145550100',
        '13:00',
        '16:00',
        'Use loading bay 3.',
        '1 W 3rd St',
        '74103',
        'Tulsa Medical Receiving',
        'Receiving Desk',
        '+19185550100',
        '08:00',
        '10:00',
        'Proof of delivery required.',
        2,
        24,
        40,
        30,
        36,
        FALSE,
        TRUE,
        FALSE,
        FALSE,
        NOW(),
        NOW()
    );

INSERT INTO offers (
    id,
    load_id,
    fleet_user_id,
    amount,
    message,
    status,
    created_at,
    updated_at
)
VALUES (
    '70000000-0000-0000-0000-000000000001',
    '60000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000003',
    2350.00,
    'Blue Ridge can cover this with a 26ft box truck and team dispatch support.',
    'PENDING',
    NOW(),
    NOW()
);

INSERT INTO conversations (
    id,
    load_id,
    broker_company_id,
    fleet_company_id,
    created_at,
    updated_at
)
VALUES (
    '80000000-0000-0000-0000-000000000001',
    '60000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    NOW(),
    NOW()
);

INSERT INTO messages (
    id,
    conversation_id,
    sender_user_id,
    sender_company_id,
    body,
    read_at,
    created_at,
    updated_at
)
VALUES
    (
        '81000000-0000-0000-0000-000000000001',
        '80000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000003',
        '10000000-0000-0000-0000-000000000002',
        'We can cover this lane. Is the dock high requirement strict?',
        NOW(),
        NOW(),
        NOW()
    ),
    (
        '81000000-0000-0000-0000-000000000002',
        '80000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        'Yes, shipper prefers dock high. Pickup appointment is flexible between 8 and 11.',
        NULL,
        NOW(),
        NOW()
    );

INSERT INTO support_tickets (
    id,
    created_at,
    updated_at,
    user_id,
    company_id,
    category,
    priority,
    subject,
    message,
    status,
    expected_response_at
)
VALUES (
    '90000000-0000-0000-0000-000000000001',
    NOW(),
    NOW(),
    '20000000-0000-0000-0000-000000000004',
    '10000000-0000-0000-0000-000000000003',
    'ONBOARDING',
    'NORMAL',
    'Bond document review',
    'We uploaded our bond paperwork. Can operations confirm if anything else is needed?',
    'WAITING_ADMIN',
    NOW() + INTERVAL '1 day'
);

INSERT INTO support_ticket_messages (
    id,
    created_at,
    updated_at,
    ticket_id,
    sender_user_id,
    sender_type,
    message
)
VALUES (
    '91000000-0000-0000-0000-000000000001',
    NOW(),
    NOW(),
    '90000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000004',
    'USER',
    'We uploaded our bond paperwork. Can operations confirm if anything else is needed?'
);

COMMIT;
