require 'digest/sha1'

require 'sinatra'
require 'mongo'
require 'sinatra/contrib'
require 'json'
require 'net/smtp'

class WatchDogServer < Sinatra::Base
  include Mongo

  def mongo
    MongoClient.new("localhost", 27017)
  end

  ## The API
  before  do
    @db ||= mongo.db('watchdog')
  end

  after do
    @db.connection.close
    @db = nil
  end


  # Do not support static files
  set :static, false

  # Enable request logging
  enable :logging

  get '/' do
    'Woof Woof'
  end

  # Get info about stored user
  get '/user/:id' do
    stored_user = get_user_by_id(params[:'id'])

    if stored_user.nil?
      halt 404, "User does not exist"
    else
      status 200
      body stored_user.to_json
    end
  end

  # Create a new user and return unique SHA1
  post '/user' do
    user = create_json_object(request)
    sha = create_40_char_SHA    

    user['id'] = sha
    user['registrationDate'] = Time.now

    users.save(user)
    stored_user = get_user_by_id(sha)

    unless user['email'].nil? or user['email'].empty?
      send_registration_email(USER_REGISTERED, user['email'], sha, nil)
    end

    status 201
    body stored_user['id']
  end

  # Create a new project and return unique SHA1
  post '/project' do
    project = create_json_object(request)

    associated_user = get_user_by_id(project['user_id'])
    if associated_user.nil?
      halt 404, "The user who registers the project does not exist on the server. Create a new user first."
    end

    sha = create_40_char_SHA()

    project['id'] = sha
    project['registrationDate'] = Time.now
    projects.save(project)

    unless associated_user['email'].nil? or associated_user['email'].empty?
      send_registration_email(PROJECT_REGISTERED, associated_user['email'], sha, project['name'])
    end

    status 201
    body sha
  end

  # Create new intervals
  post '/user/:id/intervals' do
    ivals = create_json_object(request)

    unless ivals.kind_of?(Array)
      halt 400, 'Wrong request, body is not a JSON array'
    end

    if ivals.size > 1000
      halt 400, 'Request too long (> 1000 intervals)'
    end

    negative_intervals = ivals.find{|x| (x['te'].to_i - x['ts'].to_i) < 0}

    unless negative_intervals.nil?
      halt 400, 'Request contains negative intervals'
    end

    user_id = params[:id]
    user = get_user_by_id(user_id)

    if user.nil?
      halt 404, "User does not exist"
    end

    ivals.each do |i|
      intervals.save(i)
    end

    status 201
    body ivals.size.to_s
  end

  private

  def users
    @db.collection('users')
  end

  def projects
    @db.collection('projects')
  end

  def intervals
    @db.collection('intervals')
  end

  def get_user_by_id(id)
    users.find_one({'id' => id})
  end

  def get_user_by_unq(unq)
    users.find_one({'unq' => unq})
  end

  # creates a json object from a http request
  def create_json_object(request)
     begin
      object = JSON.parse(request.body.read)
    rescue Exception => e
      logger.error e
      halt 400, "Wrong JSON object #{request.body.read}"
    end
    return object
  end

  # creates a 40 character long SHA hash
  def create_40_char_SHA()
    rnd = (0...100).map { ('a'..'z').to_a[rand(26)] }.join
    return Digest::SHA1.hexdigest rnd
  end

  def send_registration_email(mailtext, email, id, projectname)
    text = sprintf(mailtext, Time.now.rfc2822, id, projectname)

    Net::SMTP.start('localhost', 25, 'testroots.org') do |smtp|
      begin
        smtp.send_message(text, 'info@testroots.org', email)
      rescue Exception => e
        logger.error "Failed to send email to #{email}: #{e.message}"
        logger.error e.backtrace.join("\n")
      end
    end
  end

  USER_REGISTERED = <<-END_EMAIL
Subject: Your new Watchdog user id
Date: %s

Dear Watchdog user,

You recently registered with Watchdog.

Your user id is: %s

You can use this id to link other Watchdog installations to your user.

Thank you for contributing to science -- with Watchdog!

The TestRoots team
http://www.testroots.org - http://www.tudelft.nl
  END_EMAIL


  PROJECT_REGISTERED = <<-END_EMAIL
Subject: Your new Watchdog project id
Date: %s

Dear Watchdog user,

You recently registered a new project with the Watchdog.

Your new project-id is: %s
Your project name: %s

You can use this id for other workspaces where you work on the same project.
If your colleagues work on the same project, please ask them to create new
project-ids, but with the same project name.

Thank you for contributing to science -- with Watchdog!

The TestRoots team
http://www.testroots.org - http://www.tudelft.nl
  END_EMAIL

end
