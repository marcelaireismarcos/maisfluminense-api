<?php
	include 'connection.php';

	$pacote = $_POST['pacote'];

	$query = mysqli_query($con, "DELETE FROM msgpacotes WHERE pacote = '$pacote' ");

	if($query){
	  $response['success'] = 'true';
	  $response['message'] = 'Data Deleted Successfully';
	}else{
	  $response['success'] = 'false';
	  $response['message'] = 'Data Deletion Failed';
	}

	echo json_encode($response);

	mysqli_close($con);
?>
